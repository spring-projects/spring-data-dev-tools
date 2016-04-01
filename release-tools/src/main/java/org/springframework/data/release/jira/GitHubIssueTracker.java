/*
 * Copyright 2014-2016 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.git.GitProperties;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

import lombok.RequiredArgsConstructor;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RequiredArgsConstructor
class GitHubIssueTracker implements IssueTracker {

	private static final String MILESTONE_URI = "{githubBaseUrl}/repos/spring-projects/{repoName}/milestones?state={state}";
	private static final String ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues?milestone={id}&state=all&assignee={assignee}";
	private static final String ISSUES_BY_MILESTONE_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues?milestone={id}&state=all";
	private static final String MILESTONES_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/milestones";
	private static final String ISSUE_BY_ID_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues/{id}";
	private static final String ISSUES_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues";

	private static final ParameterizedTypeReference<List<GitHubIssue.Milestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<GitHubIssue.Milestone>>() {};
	private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUES_TYPE = new ParameterizedTypeReference<List<GitHubIssue>>() {};
	private static final ParameterizedTypeReference<GitHubIssue> ISSUE_TYPE = new ParameterizedTypeReference<GitHubIssue>() {};

	private final RestOperations operations;
	private final Logger logger;
	private final GitProperties properties;

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
		for (String ticketId : ticketIds) {

			Map<String, Object> parameters = newUrlTemplateVariables();
			parameters.put("repoName", repositoryName);
			parameters.put("id", ticketId);

			try {
				GitHubIssue gitHubIssue = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.GET,
						new HttpEntity<>(newUserScopedHttpHeaders()), ISSUE_TYPE, parameters).getBody();

				tickets.add(toTicket(gitHubIssue));
			} catch (HttpStatusCodeException e) {
				if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
					continue;
				}
				throw e;
			}
		}

		return tickets;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getChangelogFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("changelogs")
	public Changelog getChangelogFor(ModuleIteration moduleIteration) {

		Tickets tickets = getIssuesFor(moduleIteration, false).stream().//
				map(issue -> toTicket(issue)).//
				collect(Tickets.toTicketsCollector());

		logger.log(moduleIteration, "Created changelog with %s entries.", tickets.getOverallTotal());

		return new Changelog(moduleIteration, tickets);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return project.uses(Tracker.GITHUB);
	}

	@Cacheable("tickets")
	public Tickets getTicketsFor(ModuleIteration iteration) {
		return getTicketsFor(iteration, false);
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

	@Override
	public void createReleaseVersion(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		Optional<GitHubIssue.Milestone> milestone = findMilestone(moduleIteration, repositoryName);

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

	@Override
	public void createReleaseTicket(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		HttpHeaders httpHeaders = newUserScopedHttpHeaders();

		Tickets tickets = getTicketsFor(moduleIteration);

		if (tickets.hasReleaseTicket(moduleIteration)) {
			return;
		}

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		GitHubIssue.Milestone milestone = getMilestone(moduleIteration, repositoryName);

		logger.log(moduleIteration, "Creating release ticket…");

		GitHubIssue gitHubIssue = new GitHubIssue(Tracker.releaseTicketSummary(moduleIteration), milestone);

		operations.exchange(ISSUES_URI_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(gitHubIssue, httpHeaders),
				GitHubIssue.class, parameters).getBody();

	}

	private Tickets getTicketsFor(ModuleIteration moduleIteration, boolean forCurrentUser) {

		List<GitHubIssue> issues = getIssuesFor(moduleIteration, forCurrentUser);
		return issues.stream().map(this::toTicket).collect(Tickets.toTicketsCollector());
	}

	private List<GitHubIssue> getIssuesFor(ModuleIteration moduleIteration, boolean forCurrentUser) {

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		GitHubIssue.Milestone milestone = getMilestone(moduleIteration, repositoryName);

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());

		if (forCurrentUser) {
			parameters.put("assignee", properties.getUsername());

			return operations.exchange(ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE, HttpMethod.GET,
					new HttpEntity<>(newUserScopedHttpHeaders()), ISSUES_TYPE, parameters).getBody();
		}

		return operations.exchange(ISSUES_BY_MILESTONE_URI_TEMPLATE, HttpMethod.GET,
				new HttpEntity<>(newUserScopedHttpHeaders()), ISSUES_TYPE, parameters).getBody();
	}

	private GitHubIssue.Milestone getMilestone(ModuleIteration moduleIteration, String repositoryName) {

		Optional<GitHubIssue.Milestone> milestone = findMilestone(moduleIteration, repositoryName);
		return milestone
				.orElseThrow(() -> new IllegalStateException(String.format("No milestone for %s found containing %s!", //
						moduleIteration.getProject().getFullName(), //
						moduleIteration.getShortVersionString())));
	}

	@Cacheable("milestone")
	protected Optional<GitHubIssue.Milestone> findMilestone(ModuleIteration moduleIteration, String repositoryName) {

		for (String state : Arrays.asList("close", "open")) {

			Map<String, Object> parameters = newUrlTemplateVariables();
			parameters.put("repoName", repositoryName);
			parameters.put("state", state);

			URI milestoneUri = new UriTemplate(MILESTONE_URI).expand(parameters);

			logger.log(moduleIteration, "Looking up milestone from %s…", milestoneUri);

			List<GitHubIssue.Milestone> milestones = operations.exchange(MILESTONE_URI, HttpMethod.GET,
					new HttpEntity<>(newUserScopedHttpHeaders()), MILESTONES_TYPE, parameters).getBody();

			Optional<GitHubIssue.Milestone> milestone = milestones.stream(). //
					filter(m -> m.matchesIteration(moduleIteration)). //
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

	private HttpHeaders newUserScopedHttpHeaders() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", properties.getHttpCredentials().toString());

		return headers;
	}

	private Map<String, Object> newUrlTemplateVariables() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("githubBaseUrl", properties.getGithubApiBaseUrl());
		return parameters;
	}

	private Ticket toTicket(GitHubIssue issue) {
		return new Ticket(issue.getId(), issue.getTitle(), new GithubTicketStatus(issue.getState()));
	}

	public static void main(String[] args) {

		try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"META-INF/spring/spring-shell-plugin.xml")) {

			IssueTracker tracker = context.getBean("gitHubIssueTracker", IssueTracker.class);

			TrainIteration iteration = new TrainIteration(ReleaseTrains.CODD, Iteration.SR2);
			ModuleIteration module = iteration.getModule(Projects.BUILD);

			Changelog changelog = tracker.getChangelogFor(module);
			System.out.println(changelog);

			System.out.println(tracker.getReleaseTicketFor(module));
		}
	}
}
