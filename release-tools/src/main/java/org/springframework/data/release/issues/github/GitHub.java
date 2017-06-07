/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.release.issues.github;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.issues.Changelog;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.issues.github.GitHubIssue.Milestone;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Component
class GitHub implements IssueTracker {

	private static final String MILESTONE_URI = "{githubBaseUrl}/repos/spring-projects/{repoName}/milestones?state={state}";
	private static final String ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues?milestone={id}&state=all&assignee={assignee}";
	private static final String ISSUES_BY_MILESTONE_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues?milestone={id}&state=all";
	private static final String MILESTONES_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/milestones";
	private static final String ISSUE_BY_ID_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues/{id}";
	private static final String ISSUES_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues";

	private static final ParameterizedTypeReference<List<Milestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<Milestone>>() {};
	private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUES_TYPE = new ParameterizedTypeReference<List<GitHubIssue>>() {};
	private static final ParameterizedTypeReference<GitHubIssue> ISSUE_TYPE = new ParameterizedTypeReference<GitHubIssue>() {};

	private final RestOperations operations;
	private final Logger logger;
	private final GitHubProperties properties;

	/**
	 * @param operations
	 * @param logger
	 * @param properties
	 */
	public GitHub(@Qualifier("tracker") RestOperations operations, Logger logger, GitHubProperties properties) {

		this.operations = operations;
		this.logger = logger;
		this.properties = properties;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#flushTickets()
	 */
	@Override
	@CacheEvict(value = { "tickets", "release-tickets", "milestone" }, allEntries = true)
	public void reset() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getReleaseTicketFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("release-tickets")
	public Ticket getReleaseTicketFor(ModuleIteration module) {
		return getTicketsFor(module).getReleaseTicket(module);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IssueTracker#findTickets(Project, Collection)
	 */
	@Override
	@Cacheable("tickets")
	public Collection<Ticket> findTickets(Project project, Collection<String> ticketIds) {

		String repositoryName = GitProject.of(project).getRepositoryName();
		List<Ticket> tickets = new ArrayList<>();

		ticketIds.forEach(ticketId -> {

			Map<String, Object> parameters = newUrlTemplateVariables();
			parameters.put("repoName", repositoryName);
			parameters.put("id", ticketId);

			try {

				GitHubIssue gitHubIssue = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.GET,
						new HttpEntity<>(newUserScopedHttpHeaders()), ISSUE_TYPE, parameters).getBody();

				tickets.add(toTicket(gitHubIssue));

			} catch (HttpStatusCodeException e) {

				if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
					return;
				}

				throw e;
			}
		});

		return tickets;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getChangelogFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("changelogs")
	public Changelog getChangelogFor(ModuleIteration moduleIteration) {

		Tickets tickets = getIssuesFor(moduleIteration, false).//
				map(issue -> toTicket(issue)).//
				collect(Tickets.toTicketsCollector());

		logger.log(moduleIteration, "Created changelog with %s entries.", tickets.getOverallTotal());

		return Changelog.of(moduleIteration, tickets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return project.uses(Tracker.GITHUB);
	}

	@Override
	public Tickets getTicketsFor(TrainIteration iteration) {
		return getTicketsFor(iteration, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.GitHubIssueConnector#getTicketsFor(org.springframework.data.release.model.TrainIteration, boolean)
	 */
	@Override
	public Tickets getTicketsFor(TrainIteration trainIteration, boolean forCurrentUser) {

		if (forCurrentUser) {
			logger.log(trainIteration, "Retrieving tickets (for user %s)…", properties.getUsername());
		} else {
			logger.log(trainIteration, "Retrieving tickets…");
		}

		Tickets tickets = trainIteration.stream(). //
				filter(moduleIteration -> supports(moduleIteration.getProject())). //
				flatMap(moduleIteration -> getTicketsFor(moduleIteration, forCurrentUser).stream()). //
				collect(Tickets.toTicketsCollector());

		return tickets;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.tracker.IssueTracker#createReleaseVersion(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void createReleaseVersion(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();
		Optional<Milestone> milestone = findMilestone(moduleIteration, repositoryName);

		if (milestone.isPresent()) {
			return;
		}

		GithubMilestone githubMilestone = new GithubMilestone(moduleIteration);
		logger.log(moduleIteration, "Creating GitHub milestone %s", githubMilestone);

		HttpHeaders httpHeaders = newUserScopedHttpHeaders();
		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		operations.exchange(MILESTONES_URI_TEMPLATE, HttpMethod.POST,
				new HttpEntity<Object>(githubMilestone.toMilestone(), httpHeaders), GitHubIssue.Milestone.class, parameters);
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.springframework.data.release.tracker.IssueTracker#createReleaseTicket(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void createReleaseTicket(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		HttpHeaders httpHeaders = newUserScopedHttpHeaders();
		Tickets tickets = getTicketsFor(moduleIteration);

		if (tickets.hasReleaseTicket(moduleIteration)) {
			return;
		}

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();
		Milestone milestone = getMilestone(moduleIteration, repositoryName);
		GitHubIssue gitHubIssue = GitHubIssue.of(Tracker.releaseTicketSummary(moduleIteration), milestone);

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		logger.log(moduleIteration, "Creating release ticket…");

		operations.exchange(ISSUES_URI_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(gitHubIssue, httpHeaders),
				GitHubIssue.class, parameters).getBody();
	}

	@Cacheable("tickets")
	public Tickets getTicketsFor(ModuleIteration iteration) {
		return getTicketsFor(iteration, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#assignTicketToMe(org.springframework.data.release.jira.Ticket)
	 */
	@Override
	public void assignTicketToMe(Ticket ticket) {
		logger.log("Ticket", "Skipping ticket assignment for %s", ticket);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#assignReleaseTicketToMe(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public Ticket assignReleaseTicketToMe(ModuleIteration module) {

		Assert.notNull(module, "ModuleIteration must not be null.");

		Ticket releaseTicketFor = getReleaseTicketFor(module);
		String repositoryName = GitProject.of(module.getProject()).getRepositoryName();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", stripHash(releaseTicketFor));

		GitHubIssue edit = GitHubIssue.assignedTo(properties.getUsername());

		GitHubIssue response = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH,
				new HttpEntity<>(edit, newUserScopedHttpHeaders()), ISSUE_TYPE, parameters).getBody();

		return toTicket(response);
	}

	private String stripHash(Ticket ticket) {
		return ticket.getId().startsWith("#") ? ticket.getId().substring(1) : ticket.getId();
	}

	private HttpHeaders newUserScopedHttpHeaders() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", properties.getHttpCredentials().toString());

		return headers;
	}

	private Map<String, Object> newUrlTemplateVariables() {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("githubBaseUrl", properties.getApiUrl());

		return parameters;
	}

	private Optional<Milestone> findMilestone(ModuleIteration moduleIteration, String repositoryName) {

		for (String state : Arrays.asList("close", "open")) {

			Map<String, Object> parameters = newUrlTemplateVariables();
			parameters.put("repoName", repositoryName);
			parameters.put("state", state);

			URI milestoneUri = new UriTemplate(MILESTONE_URI).expand(parameters);

			logger.log(moduleIteration, "Looking up milestone from %s…", milestoneUri);

			List<GitHubIssue.Milestone> milestones = operations.exchange(MILESTONE_URI, HttpMethod.GET,
					new HttpEntity<>(newUserScopedHttpHeaders()), MILESTONES_TYPE, parameters).getBody();

			Optional<GitHubIssue.Milestone> milestone = milestones.stream(). //
					filter(m -> m.matches(moduleIteration)). //
					findFirst(). //
					map(m -> {
						logger.log(moduleIteration, "Found milestone %s.", m);
						return m;
					});

			if (milestone.isPresent()) {
				return milestone;
			}
		}

		return Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.issues.IssueTracker#closeIteration(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void closeIteration(ModuleIteration module) {

	}

	private Tickets getTicketsFor(ModuleIteration moduleIteration, boolean forCurrentUser) {

		return getIssuesFor(moduleIteration, forCurrentUser).//
				map(GitHub::toTicket).//
				collect(Tickets.toTicketsCollector());
	}

	private Stream<GitHubIssue> getIssuesFor(ModuleIteration moduleIteration, boolean forCurrentUser) {

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		GitHubIssue.Milestone milestone = getMilestone(moduleIteration, repositoryName);

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());

		if (forCurrentUser) {
			parameters.put("assignee", properties.getUsername());

			return getForIssues(ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE, parameters);
		}

		return getForIssues(ISSUES_BY_MILESTONE_URI_TEMPLATE, parameters);
	}

	private Stream<GitHubIssue> getForIssues(String template, Map<String, Object> parameters) {

		return operations
				.exchange(template, HttpMethod.GET, new HttpEntity<>(newUserScopedHttpHeaders()), ISSUES_TYPE, parameters)
				.getBody().stream();
	}

	private Milestone getMilestone(ModuleIteration moduleIteration, String repositoryName) {

		Optional<Milestone> milestone = findMilestone(moduleIteration, repositoryName);

		return milestone
				.orElseThrow(() -> new IllegalStateException(String.format("No milestone for %s found containing %s!", //
						moduleIteration.getProject().getFullName(), //
						moduleIteration.getShortVersionString())));
	}

	private static Ticket toTicket(GitHubIssue issue) {
		return new Ticket(issue.getId(), issue.getTitle(), new GithubTicketStatus(issue.getState()));
	}
}
