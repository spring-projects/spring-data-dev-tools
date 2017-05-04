/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.issues.jira;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.issues.Changelog;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.issues.jira.JiraIssue.Fields;
import org.springframework.data.release.issues.jira.JiraIssue.Resolution;
import org.springframework.data.release.issues.jira.JiraIssue.Status;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ProjectKey;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Component
class Jira implements JiraConnector {

	private static final String BASE_URI = "{jiraBaseUrl}/rest/api/2";
	private static final String CREATE_ISSUES_TEMPLATE = BASE_URI + "/issue";
	private static final String UPDATE_ISSUE_TEMPLATE = BASE_URI + "/issue/{ticketId}";
	private static final String PROJECT_VERSIONS_TEMPLATE = BASE_URI + "/project/{project}/version?startAt={startAt}";
	private static final String PROJECT_COMPONENTS_TEMPLATE = BASE_URI + "/project/{project}/components";
	private static final String VERSIONS_TEMPLATE = BASE_URI + "/version";
	private static final String VERSION_TEMPLATE = BASE_URI + "/version/{id}";
	private static final String SEARCH_TEMPLATE = BASE_URI + "/search?jql={jql}&fields={fields}&startAt={startAt}";

	public static final String INFRASTRUCTURE_COMPONENT_NAME = "Infrastructure";

	private final RestOperations operations;
	private final Logger logger;
	private final JiraProperties jiraProperties;

	/**
	 * @param operations
	 * @param logger
	 * @param jiraProperties
	 */
	public Jira(@Qualifier("tracker") RestOperations operations, Logger logger, JiraProperties jiraProperties) {

		this.operations = operations;
		this.logger = logger;
		this.jiraProperties = jiraProperties;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#reset()
	 */
	@Override
	@CacheEvict(value = { "release-ticket", "tickets", "changelogs", "release-version" }, allEntries = true)
	public void reset() {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getReleaseTicketFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("release-ticket")
	public Ticket getReleaseTicketFor(ModuleIteration moduleIteration) {

		JqlQuery query = JqlQuery.from(moduleIteration)
				.and(String.format("summary ~ \"%s\"", Tracker.releaseTicketSummary(moduleIteration)));

		JiraIssues issues = getJiraIssues(query, new HttpHeaders(), 0);

		if (!issues.hasIssues()) {
			throw new IllegalArgumentException(String.format("Did not find a release ticket for %s!", moduleIteration));
		}

		return toTicket(issues.stream().findFirst().get());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.data.release.jira.IssueTracker#findTickets(Project, Collection)
	 */
	@Override
	@Cacheable("tickets")
	public Collection<Ticket> findTickets(Project project, Collection<String> ticketIds) {

		if (ticketIds.isEmpty()) {
			return Collections.emptyList();
		}

		JqlQuery query = JqlQuery.from(ticketIds).and(" resolution is not EMPTY");

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("jql", query);
		parameters.put("fields", "summary,status,resolution");
		parameters.put("startAt", 0);

		JiraIssues issues = operations.exchange(SEARCH_TEMPLATE, HttpMethod.GET, null, JiraIssues.class, parameters)
				.getBody();

		return issues.stream().//
				map(this::toTicket).//
				collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getTicketsFor(org.springframework.data.release.model.TrainIteration)
	 */
	@Override
	@Cacheable("tickets")
	public Tickets getTicketsFor(TrainIteration iteration) {
		return getTicketsFor(iteration, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getTicketsFor(org.springframework.data.release.model.TrainIteration, boolean)
	 */
	@Override
	@Cacheable("tickets")
	public Tickets getTicketsFor(TrainIteration trainIteration, boolean forCurrentUser) {

		JqlQuery query = JqlQuery
				.from(trainIteration.stream().filter(moduleIteration -> supports(moduleIteration.getProject())));

		HttpHeaders headers = newUserScopedHttpHeaders();

		List<Ticket> tickets = new ArrayList<>();

		if (forCurrentUser) {
			query = query.and("assignee = currentUser()");
			logger.log(trainIteration, "Retrieving tickets (for user %s)…", jiraProperties.getUsername());
		} else {
			logger.log(trainIteration, "Retrieving tickets…");
		}

		query = query.orderBy("updatedDate DESC");

		JiraIssues issues = execute(trainIteration.toString(), query, headers, jiraIssues -> {
			jiraIssues.stream().//
			filter(jiraIssue -> !jiraIssue.wasBackportedFrom(trainIteration.getTrain())). //
			forEach(jiraIssue -> tickets.add(toTicket(jiraIssue)));
		});

		return new Tickets(tickets, issues.getTotal());
	}

	@Cacheable("tickets")
	public Tickets getTicketsFor(ModuleIteration moduleIteration) {

		JqlQuery query = JqlQuery.from(moduleIteration);

		HttpHeaders headers = new HttpHeaders();

		List<Ticket> tickets = new ArrayList<>();

		logger.log(moduleIteration, "Retrieving tickets…");

		query = query.orderBy("updatedDate DESC");

		JiraIssues issues = execute(moduleIteration.toString(), query, headers, jiraIssues -> {
			jiraIssues.stream().//
			filter(jiraIssue -> !jiraIssue.wasBackportedFrom(moduleIteration.getTrain())). //
			forEach(jiraIssue -> tickets.add(toTicket(jiraIssue)));
		});

		return new Tickets(tickets, issues.getTotal());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#createReleaseVersion(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseVersion(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		Map<String, Object> parameters = newUrlTemplateVariables();
		HttpHeaders httpHeaders = newUserScopedHttpHeaders();

		Optional<JiraReleaseVersion> versionsForModuleIteration = findJiraReleaseVersion(moduleIteration);

		if (versionsForModuleIteration.isPresent()) {
			return;
		}

		JiraVersion jiraVersion = new JiraVersion(moduleIteration);
		logger.log(moduleIteration, "Creating Jira release version %s", jiraVersion);

		JiraReleaseVersion jiraReleaseVersion = JiraReleaseVersion.of(moduleIteration, jiraVersion);

		operations.exchange(VERSIONS_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(jiraReleaseVersion, httpHeaders),
				JiraReleaseVersion.class, parameters).getBody();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#findJiraReleaseVersion(org.springframework.data.release.model.ModuleIteration)
	 */
	@Cacheable("release-version")
	public Optional<JiraReleaseVersion> findJiraReleaseVersion(ModuleIteration moduleIteration) {

		JiraVersion jiraVersion = new JiraVersion(moduleIteration);
		HttpHeaders httpHeaders = new HttpHeaders();

		List<JiraReleaseVersion> versionsForModuleIteration = new ArrayList<>();
		getReleaseVersions(moduleIteration.toString(), moduleIteration.getProjectKey(), httpHeaders, releaseVersions -> {
			releaseVersions.stream(). //
			filter(jiraReleaseVersion -> jiraReleaseVersion.hasSameNameAs(jiraVersion)). //
			findFirst(). //
			ifPresent(jiraReleaseVersion -> versionsForModuleIteration.add(jiraReleaseVersion));
		});

		return versionsForModuleIteration.stream().findFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#createReleaseTicket(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void createReleaseTicket(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		HttpHeaders httpHeaders = newUserScopedHttpHeaders();

		Map<String, Object> parameters = newUrlTemplateVariables();

		Tickets tickets = getTicketsFor(moduleIteration);

		if (tickets.hasReleaseTicket(moduleIteration)) {
			return;
		}

		findJiraReleaseVersion(moduleIteration).orElseThrow(
				() -> new IllegalStateException(String.format("Did not find a release version for %s", moduleIteration)));

		JiraComponents jiraComponents = getJiraComponents(moduleIteration.getProjectKey());

		logger.log(moduleIteration, "Creating release ticket…");

		JiraIssue jiraIssue = prepareJiraIssueToCreate(moduleIteration, jiraComponents);

		operations.exchange(CREATE_ISSUES_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(jiraIssue, httpHeaders),
				CreatedJiraIssue.class, parameters).getBody();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#assignTicketToMe(org.springframework.data.release.jira.Ticket)
	 */
	@Override
	public void assignTicketToMe(Ticket ticket) {

		Assert.notNull(ticket, "Ticket must not be null.");

		HttpHeaders httpHeaders = newUserScopedHttpHeaders();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("ticketId", ticket.getId());

		JiraIssue jiraIssue = JiraIssue.create().assignTo(jiraProperties.getCredentials());

		operations.exchange(UPDATE_ISSUE_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(jiraIssue, httpHeaders),
				String.class, parameters).getBody();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#assignReleaseTicketToMe(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public Ticket assignReleaseTicketToMe(ModuleIteration module) {

		Ticket ticket = getReleaseTicketFor(module);
		assignTicketToMe(ticket);
		return ticket;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#verifyBeforeRelease(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration)
	 */
	@Override
	public void verifyBeforeRelease(TrainIteration trainIteration) {

		// for each module
		for (ModuleIteration moduleIteration : trainIteration) {

			Tickets tickets = getTicketsFor(moduleIteration);
			Ticket releaseTicket = tickets.getReleaseTicket(moduleIteration);

			if (releaseTicket.isResolved()) {
				throw new IllegalStateException(
						String.format("Release ticket %s for %s is resolved", releaseTicket, moduleIteration));
			}

			Tickets issueTickets = tickets.getIssueTickets(moduleIteration);

			List<Ticket> unresolvedTickets = issueTickets.stream().//
					filter(ticket -> !ticket.isResolved()).//
					collect(Collectors.toList());

			if (!unresolvedTickets.isEmpty()) {
				throw new IllegalStateException(
						String.format("Unresolved tickets for %s: %s", moduleIteration, unresolvedTickets));
			}
		}
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#closeIteration(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration)
	 */
	@Override
	public void closeIteration(ModuleIteration module) {

		// for each module

		// - close all tickets
		// -- make sure only one ticket is open
		// -- resolve open ticket
		// -- close tickets

		// - mark version as releases

		HttpHeaders httpHeaders = newUserScopedHttpHeaders();

		findJiraReleaseVersion(module) //
				.filter(JiraReleaseVersion::isOpen) //
				.map(JiraReleaseVersion::markReleased) //
				.ifPresent(version -> {

					logger.log(module, "Marking version %s as released.", version);

					Map<String, Object> parameters = newUrlTemplateVariables();
					parameters.put("id", version.getId());

					operations.exchange(VERSION_TEMPLATE, HttpMethod.PUT, new HttpEntity<Object>(version, httpHeaders), Map.class,
							parameters);
				});

		// - if no next version exists, create
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#getChangelogFor(org.springframework.data.release.model.Module, org.springframework.data.release.model.Iteration)
	 */
	@Override
	@Cacheable("changelogs")
	public Changelog getChangelogFor(ModuleIteration moduleIteration) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("jql", JqlQuery.from(moduleIteration));
		parameters.put("fields", "summary,status,resolution,fixVersions");
		parameters.put("startAt", 0);

		URI searchUri = new UriTemplate(SEARCH_TEMPLATE).expand(parameters);

		logger.log(moduleIteration, "Looking up JIRA issues from %s…", searchUri);

		JiraIssues issues = operations.getForObject(searchUri, JiraIssues.class);
		Tickets tickets = issues.stream().map(this::toTicket).collect(Tickets.toTicketsCollector());
		logger.log(moduleIteration, "Created changelog with %s entries.", tickets.getOverallTotal());

		return Changelog.of(moduleIteration, tickets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return project.uses(Tracker.JIRA);
	}

	@Cacheable("jira-components")
	protected JiraComponents getJiraComponents(ProjectKey projectKey) {

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("project", projectKey.getKey());

		List<JiraComponent> components = operations.exchange(PROJECT_COMPONENTS_TEMPLATE, HttpMethod.GET,
				new HttpEntity<>(headers), new ParameterizedTypeReference<List<JiraComponent>>() {}, parameters).getBody();

		return JiraComponents.of(components);
	}

	private JiraIssue prepareJiraIssueToCreate(ModuleIteration moduleIteration, JiraComponents jiraComponents) {

		JiraIssue jiraIssue = JiraIssue.createTask();
		jiraIssue.project(moduleIteration.getProjectKey()).summary(Tracker.releaseTicketSummary(moduleIteration))
				.fixVersion(moduleIteration);

		Fields fields = jiraIssue.getFields();

		Optional<JiraComponent> component = jiraComponents.findComponent(INFRASTRUCTURE_COMPONENT_NAME);
		component.ifPresent(jiraComponent -> fields.setComponents(Collections
				.singletonList(org.springframework.data.release.issues.jira.JiraIssue.Component.of(jiraComponent.getName()))));

		return jiraIssue;
	}

	private JiraIssues execute(String context, JqlQuery query, HttpHeaders headers, JiraIssuesCallback callback) {

		JiraIssues issues;
		int startAt = 0;

		do {
			issues = getJiraIssues(query, headers, startAt);

			logger.log(context, "Got tickets %s to %s of %s.", startAt, issues.getNextStartAt(), issues.getTotal());

			callback.doWithJiraIssues(issues);

			startAt = issues.getNextStartAt();

		} while (issues.hasMoreResults());

		return issues;
	}

	private JiraIssues getJiraIssues(JqlQuery query, HttpHeaders headers, int startAt) {

		JiraIssues issues;
		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("jql", query);
		parameters.put("fields", "summary,status,resolution,fixVersions");
		parameters.put("startAt", startAt);

		issues = operations
				.exchange(SEARCH_TEMPLATE, HttpMethod.GET, new HttpEntity<>(headers), JiraIssues.class, parameters).getBody();
		return issues;
	}

	private JiraReleaseVersions getReleaseVersions(String context, ProjectKey projectKey, HttpHeaders headers,
			JiraReleaseVersionsCallback callback) {

		JiraReleaseVersions releaseVersions;
		int startAt = 0;

		do {

			releaseVersions = getJiraReleaseVersions(projectKey, headers, startAt);

			logger.log(context, "Got release versions %s to %s of %s.", startAt, releaseVersions.getNextStartAt(),
					releaseVersions.getTotal());

			callback.doWithJiraReleaseVersions(releaseVersions);

			startAt = releaseVersions.getNextStartAt();

		} while (releaseVersions.hasMoreResults());

		return releaseVersions;
	}

	private JiraReleaseVersions getJiraReleaseVersions(ProjectKey projectKey, HttpHeaders headers, int startAt) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("project", projectKey.getKey());
		parameters.put("fields", "summary,status,resolution,fixVersions");
		parameters.put("startAt", startAt);

		return operations.exchange(PROJECT_VERSIONS_TEMPLATE, HttpMethod.GET, new HttpEntity<>(headers),
				JiraReleaseVersions.class, parameters).getBody();
	}

	private Ticket toTicket(JiraIssue issue) {

		Fields fields = issue.getFields();
		JiraTicketStatus jiraTicketStatus;

		if (fields.hasStatusAndResolution()) {

			Status status = fields.getStatus();
			boolean resolved = status.getStatusCategory().getKey().equals("done");
			Resolution resolution = fields.getResolution();

			jiraTicketStatus = JiraTicketStatus.of(resolved, status.getName(), resolution.getName());
		} else {
			jiraTicketStatus = JiraTicketStatus.UNKNOWN;
		}

		return new Ticket(issue.getKey(), fields.getSummary(), jiraTicketStatus);
	}

	/**
	 * Returns new {@link HttpHeaders} with authentication headers.
	 * 
	 * @return
	 */
	private HttpHeaders newUserScopedHttpHeaders() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", String.format("Basic %s", jiraProperties.getCredentials().asBase64()));
		return headers;
	}

	private Map<String, Object> newUrlTemplateVariables() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("jiraBaseUrl", jiraProperties.getApiUrl());
		return parameters;
	}

	/**
	 * Callback for {@link JiraIssues}.
	 */
	interface JiraIssuesCallback {

		void doWithJiraIssues(JiraIssues jiraIssues);
	}

	/**
	 * Callback for {@link JiraReleaseVersions}.
	 */
	interface JiraReleaseVersionsCallback {

		void doWithJiraReleaseVersions(JiraReleaseVersions releaseVersions);
	}
}
