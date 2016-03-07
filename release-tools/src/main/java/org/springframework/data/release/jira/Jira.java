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
package org.springframework.data.release.jira;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.Application;
import org.springframework.data.release.jira.JiraIssue.Component;
import org.springframework.data.release.jira.JiraIssue.Fields;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ProjectKey;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

import lombok.RequiredArgsConstructor;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RequiredArgsConstructor
class Jira implements JiraConnector {

	private static final String BASE_URI = "{jiraBaseUrl}/rest/api/2";
	private static final String CREATE_ISSUES_TEMPLATE = BASE_URI + "/issue";
	private static final String UPDATE_ISSUE_TEMPLATE = BASE_URI + "/issue/{ticketId}";
	private static final String PROJECT_VERSIONS_TEMPLATE = BASE_URI + "/project/{project}/version?startAt={startAt}";
	private static final String PROJECT_COMPONENTS_TEMPLATE = BASE_URI + "/project/{project}/components";
	private static final String VERSIONS_TEMPLATE = BASE_URI + "/version";
	private static final String SEARCH_TEMPLATE = BASE_URI + "/search?jql={jql}&fields={fields}&startAt={startAt}";

	public static final String INFRASTRUCTURE_COMPONENT_NAME = "Infrastructure";

	private final RestOperations operations;
	private final Logger logger;

	@Value("${jira.url}") private final String jiraBaseUrl;

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
	public Ticket getReleaseTicketFor(ModuleIteration iteration) {

		JqlQuery query = JqlQuery.from(iteration)
				.and(String.format("summary ~ \"%s\"", Tracker.releaseTicketSummary(iteration)));

		JiraIssues issues = getJiraIssues(query, new HttpHeaders(), 0);

		if (issues.getIssues().isEmpty()) {
			throw new IllegalStateException(String.format("Did not find a release ticket for %s!", iteration));
		}

		JiraIssue issue = issues.getIssues().get(0);

		return toTicket(issue);
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

		return issues.getIssues().stream().//
				map(this::toTicket).//
				collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#getTicketsFor(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	@Cacheable("tickets")
	public Tickets getTicketsFor(TrainIteration iteration, Credentials credentials) {

		JqlQuery query = JqlQuery.from(iteration);

		HttpHeaders headers = new HttpHeaders();

		List<Ticket> tickets = new ArrayList<>();

		if (credentials != null) {

			query = query.and("assignee = currentUser()");

			withAuthorization(credentials, headers);

			logger.log(iteration, "Retrieving tickets (for user %s)…", credentials.getUsername());
		} else {
			logger.log(iteration, "Retrieving tickets…");
		}

		query = query.orderBy("updatedDate DESC");

		JiraIssues issues = execute(iteration.toString(), query, headers, jiraIssues -> {
			for (JiraIssue issue : jiraIssues) {
				if (!issue.wasBackportedFrom(iteration.getTrain())) {
					tickets.add(toTicket(issue));
				}
			}
		});

		return new Tickets(Collections.unmodifiableList(tickets), issues.getTotal());
	}

	@Cacheable("tickets")
	public Tickets getTicketsFor(ModuleIteration iteration) {

		JqlQuery query = JqlQuery.from(iteration);

		HttpHeaders headers = new HttpHeaders();

		List<Ticket> tickets = new ArrayList<>();

		logger.log(iteration, "Retrieving tickets…");

		query = query.orderBy("updatedDate DESC");

		JiraIssues issues = execute(iteration.toString(), query, headers, jiraIssues -> {
			for (JiraIssue issue : jiraIssues) {
				if (!issue.wasBackportedFrom(iteration.getTrain())) {
					tickets.add(toTicket(issue));
				}
			}
		});

		return new Tickets(Collections.unmodifiableList(tickets), issues.getTotal());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#createReleaseVersions(org.springframework.data.release.model.TrainIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseVersions(TrainIteration iteration, Credentials credentials) {

		Assert.notNull(iteration, "TrainIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		for (ModuleIteration moduleIteration : iteration) {

			if (!supports(moduleIteration.getProject())) {
				continue;
			}

			createReleaseVersion(moduleIteration, credentials);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#createReleaseVersion(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseVersion(ModuleIteration moduleIteration, Credentials credentials) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		Map<String, Object> parameters = newUrlTemplateVariables();
		HttpHeaders httpHeaders = new HttpHeaders();
		withAuthorization(credentials, httpHeaders);

		Optional<JiraReleaseVersion> versionsForModuleIteration = findJiraReleaseVersion(moduleIteration);

		if (!versionsForModuleIteration.isPresent()) {

			JiraVersion jiraVersion = new JiraVersion(moduleIteration);
			logger.log(moduleIteration, "Creating Jira release version %s", jiraVersion);

			JiraReleaseVersion jiraReleaseVersion = new JiraReleaseVersion();
			jiraReleaseVersion.setProject(moduleIteration.getProjectKey().getKey());
			jiraReleaseVersion.setName(jiraVersion.toString());
			jiraReleaseVersion.setDescription(jiraVersion.getDescription());

			operations.exchange(VERSIONS_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(jiraReleaseVersion, httpHeaders),
					JiraReleaseVersion.class, parameters).getBody();
		}
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
					filter(jiraReleaseVersion -> jiraReleaseVersion.getName().equals(jiraVersion.toString())). //
					forEach(jiraReleaseVersion -> versionsForModuleIteration.add(jiraReleaseVersion));

		});

		return versionsForModuleIteration.stream().findFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#createReleaseTickets(org.springframework.data.release.model.TrainIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseTickets(TrainIteration iteration, Credentials credentials) {

		Assert.notNull(iteration, "TrainIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		for (ModuleIteration moduleIteration : iteration) {

			if (!supports(moduleIteration.getProject())) {
				continue;
			}

			createReleaseTicket(moduleIteration, credentials);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#createReleaseTicket(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseTicket(ModuleIteration moduleIteration, Credentials credentials) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		HttpHeaders httpHeaders = new HttpHeaders();
		withAuthorization(credentials, httpHeaders);
		Map<String, Object> parameters = newUrlTemplateVariables();

		Tickets tickets = getTicketsFor(moduleIteration);

		if (tickets.hasReleaseTicket(moduleIteration)) {
			return;
		}

		Optional<JiraReleaseVersion> jiraReleaseVersion = findJiraReleaseVersion(moduleIteration);
		if (!jiraReleaseVersion.isPresent()) {
			throw new IllegalStateException(String.format("Did not find a release version for %s", moduleIteration));
		}

		JiraComponents jiraComponents = getJiraComponents(moduleIteration.getProjectKey());

		logger.log(moduleIteration, "Creating release ticket…");

		JiraIssue jiraIssue = prepareJiraIssueToCreate(moduleIteration, jiraComponents);

		operations.exchange(CREATE_ISSUES_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(jiraIssue, httpHeaders),
				CreatedJiraIssue.class, parameters).getBody();

	}

	@Override
	public void assignTicketToMe(Ticket ticket, Credentials credentials) {

		Assert.notNull(ticket, "Ticket must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		HttpHeaders httpHeaders = new HttpHeaders();
		withAuthorization(credentials, httpHeaders);
		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("ticketId", ticket.getId());

		JiraIssue jiraIssue = JiraIssue.create().assignTo(credentials);

		operations.exchange(UPDATE_ISSUE_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(jiraIssue, httpHeaders),
				String.class, parameters).getBody();
	}

	private JiraIssue prepareJiraIssueToCreate(ModuleIteration moduleIteration, JiraComponents jiraComponents) {

		JiraIssue jiraIssue = JiraIssue.createTask();
		jiraIssue.project(moduleIteration.getProjectKey()).summary(Tracker.releaseTicketSummary(moduleIteration))
				.fixVersion(moduleIteration);

		Fields fields = jiraIssue.getFields();

		Optional<JiraComponent> component = jiraComponents.findComponent(INFRASTRUCTURE_COMPONENT_NAME);
		component.ifPresent(
				jiraComponent -> fields.setComponents(Collections.singletonList(Component.from(jiraComponent.getName()))));

		return jiraIssue;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#verifyBeforeRelease(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration)
	 */
	@Override
	public void verifyBeforeRelease(TrainIteration iteration) {

		// for each module
		for (ModuleIteration moduleIteration : iteration) {
			Tickets tickets = getTicketsFor(moduleIteration);

			tickets.getReleaseTicket(moduleIteration);

			Tickets issueTickets = tickets.getIssueTickets(moduleIteration);

			List<Ticket> unresolvedTickets = new ArrayList<>();
			for (Ticket ticket : issueTickets) {
				if (!ticket.getTicketStatus().isResolved()) {
					unresolvedTickets.add(ticket);
				}
			}

			if (!unresolvedTickets.isEmpty()) {
				throw new IllegalStateException(
						String.format("Unresolved tickets for %s: %s", moduleIteration, unresolvedTickets));
			}
		}
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#closeIteration(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void closeIteration(TrainIteration iteration, Credentials credentials) {

		// for each module

		// - close all tickets
		// -- make sure only one ticket is open
		// -- resolve open ticket
		// -- close tickets

		// - mark version as releases
		// - if no next version exists, create
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#getChangelogFor(org.springframework.data.release.model.Module, org.springframework.data.release.model.Iteration)
	 */
	@Override
	@Cacheable("changelogs")
	public Changelog getChangelogFor(ModuleIteration module) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("jql", JqlQuery.from(module));
		parameters.put("fields", "summary,status,resolution,fixVersions");
		parameters.put("startAt", 0);

		URI searchUri = new UriTemplate(SEARCH_TEMPLATE).expand(parameters);

		logger.log(module, "Looking up JIRA issues from %s…", searchUri);

		JiraIssues issues = operations.getForObject(searchUri, JiraIssues.class);
		List<Ticket> tickets = new ArrayList<>();

		for (JiraIssue issue : issues) {
			tickets.add(toTicket(issue));
		}

		logger.log(module, "Created changelog with %s entries.", tickets.size());

		return new Changelog(module, new Tickets(tickets, tickets.size()));
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

		return JiraComponents.from(components);
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

		JiraIssue.Fields fields = issue.getFields();

		JiraTicketStatus jiraTicketStatus;
		if (fields.getStatus() != null && fields.getResolution() != null) {
			JiraIssue.Status status = fields.getStatus();
			boolean resolved = status.getStatusCategory().getKey().equals("done");
			JiraIssue.Resolution resolution = fields.getResolution();

			jiraTicketStatus = new JiraTicketStatus(resolved, status.getName(), resolution.getName());
		} else {
			jiraTicketStatus = JiraTicketStatus.UNKNOWN;
		}

		return new Ticket(issue.getKey(), fields.getSummary(), jiraTicketStatus);
	}

	private void withAuthorization(Credentials credentials, HttpHeaders headers) {
		headers.set("Authorization", String.format("Basic %s", credentials.asBase64()));
	}

	public static void main(String[] args) {

		try (ConfigurableApplicationContext context = SpringApplication.run(Application.class, args)) {

			JiraConnector tracker = context.getBean(JiraConnector.class);
			TrainIteration iteration = new TrainIteration(ReleaseTrains.GOSLING, Iteration.SR1);
			ModuleIteration module = iteration.getModule("JPA");

			// Changelog changelog = tracker.getChangelogFor(module);
			// System.out.println(changelog);

			System.out.println(tracker.getReleaseTicketFor(module));
		}
	}

	private Map<String, Object> newUrlTemplateVariables() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("jiraBaseUrl", jiraBaseUrl);
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
