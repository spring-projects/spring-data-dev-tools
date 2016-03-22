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
import java.util.stream.Collectors;

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
class GitHubIssueTracker implements GitHubIssueConnector {

	private static final String MILESTONE_URI = "{githubBaseUrl}/repos/spring-projects/{repoName}/milestones?state={state}";
	private static final String ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues?milestone={id}&state=all&assignee={assignee}";
	private static final String ISSUES_BY_MILESTONE_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues?milestone={id}&state=all";
	private static final String MILESTONES_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/milestones";
	private static final String ISSUE_BY_ID_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues/{id}";
	private static final String ISSUES_URI_TEMPLATE = "{githubBaseUrl}/repos/spring-projects/{repoName}/issues";

	private static final ParameterizedTypeReference<List<GitHubMilestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<GitHubMilestone>>() {};
	private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUES_TYPE = new ParameterizedTypeReference<List<GitHubIssue>>() {};
	private static final ParameterizedTypeReference<GitHubIssue> ISSUE_TYPE = new ParameterizedTypeReference<GitHubIssue>() {};

	private final RestOperations operations;
	private final Logger logger;
	private final GitProperties properties;
	private final String gitubBaseUrl;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#flushTickets()
	 */
	@Override
	@CacheEvict(value = { "tickets", "release-tickets" }, allEntries = true)
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
						new HttpEntity<>(getAuthenticationHeaders()), ISSUE_TYPE, parameters).getBody();

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
	public Changelog getChangelogFor(ModuleIteration module) {

		List<Ticket> tickets = getIssuesFor(module, false).stream().//
				map(issue -> toTicket(issue)).//
				collect(Collectors.toList());

		logger.log(module, "Created changelog with %s entries.", tickets.size());

		return new Changelog(module, new Tickets(tickets));
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.GitHubIssueConnector#getTicketsFor(org.springframework.data.release.model.TrainIteration, boolean)
	 */
	@Override
	public Tickets getTicketsFor(TrainIteration iteration, boolean forCurrentUser) {

		if (forCurrentUser) {
			logger.log(iteration, "Retrieving tickets (for user %s)…", properties.getUsername());
		} else {
			logger.log(iteration, "Retrieving tickets…");
		}

		List<Ticket> tickets = iteration.stream(). //
				filter(moduleIteration -> supports(moduleIteration.getProject())). //
				flatMap(moduleIteration -> getTicketsFor(moduleIteration, forCurrentUser).stream()). //
				collect(Collectors.toList());

		return new Tickets(tickets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#createReleaseVersions(org.springframework.data.release.model.TrainIteration, org.springframework.data.release.jira.Credentials)
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
	 * @see org.springframework.data.release.jira.IssueTracker#createReleaseVersion(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseVersion(ModuleIteration moduleIteration, Credentials credentials) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		Optional<GitHubMilestone> milestone = findMilestone(moduleIteration, repositoryName);

		if (milestone.isPresent()) {
			return;
		}

		JiraVersion jiraVersion = new JiraVersion(moduleIteration);
		logger.log(moduleIteration, "Creating GitHub milestone %s", jiraVersion);

		HttpHeaders httpHeaders = getAuthenticationHeaders();
		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		GitHubMilestone gitHubMilestone = new GitHubMilestone();
		gitHubMilestone.setTitle(jiraVersion.toString());
		gitHubMilestone.setDescription(jiraVersion.getDescription());

		operations.exchange(MILESTONES_URI_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(gitHubMilestone, httpHeaders),
				GitHubMilestone.class, parameters);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#createReleaseTickets(org.springframework.data.release.model.TrainIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseTickets(TrainIteration trainIteration, Credentials credentials) {

		Assert.notNull(trainIteration, "TrainIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		for (ModuleIteration moduleIteration : trainIteration) {

			if (!supports(moduleIteration.getProject())) {
				continue;
			}

			createReleaseTicket(moduleIteration, credentials);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#createReleaseTicket(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void createReleaseTicket(ModuleIteration moduleIteration, Credentials credentials) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");
		Assert.notNull(credentials, "Credentials must not be null.");

		HttpHeaders httpHeaders = getAuthenticationHeaders();

		Tickets tickets = getTicketsFor(moduleIteration);

		if (tickets.hasReleaseTicket(moduleIteration)) {
			return;
		}

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		GitHubMilestone milestone = getMilestone(moduleIteration, repositoryName);

		logger.log(moduleIteration, "Creating release ticket…");

		GitHubIssue gitHubIssue = new GitHubIssue();
		gitHubIssue.title(Tracker.releaseTicketSummary(moduleIteration)).milestone(milestone);

		operations.exchange(ISSUES_URI_TEMPLATE, HttpMethod.POST, new HttpEntity<Object>(gitHubIssue, httpHeaders),
				GitHubIssue.class, parameters).getBody();

	}

	private Tickets getTicketsFor(ModuleIteration iteration, boolean forCurrentUser) {

		List<GitHubIssue> issues = getIssuesFor(iteration, forCurrentUser);
		List<Ticket> tickets = issues.stream().map(this::toTicket).collect(Collectors.toList());
		return new Tickets(tickets);
	}

	private List<GitHubIssue> getIssuesFor(ModuleIteration module, boolean forCurrentUser) {

		String repositoryName = GitProject.of(module.getProject()).getRepositoryName();

		GitHubMilestone milestone = getMilestone(module, repositoryName);

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());
		
		if(forCurrentUser) {
			parameters.put("assignee", properties.getUsername());

			return operations.exchange(ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE, HttpMethod.GET,
					new HttpEntity<>(getAuthenticationHeaders()), ISSUES_TYPE, parameters).getBody();
		}
		
		return operations.exchange(ISSUES_BY_MILESTONE_URI_TEMPLATE, HttpMethod.GET,
				new HttpEntity<>(getAuthenticationHeaders()), ISSUES_TYPE, parameters).getBody();
	}

	private GitHubMilestone getMilestone(ModuleIteration module, String repositoryName) {

		Optional<GitHubMilestone> milestone = findMilestone(module, repositoryName);
		if (!milestone.isPresent()) {
			throw new IllegalStateException(
					String.format("No milestone for %s found containing %s!", module.getProject().getFullName(), module.getShortVersionString()));
		}

		return milestone.get();
	}

	@Cacheable("milestone")
	private Optional<GitHubMilestone> findMilestone(ModuleIteration module, String repositoryName) {

		for (String state : Arrays.asList("close", "open")) {

			Map<String, Object> parameters = newUrlTemplateVariables();
			parameters.put("repoName", repositoryName);
			parameters.put("state", state);

			URI milestoneUri = new UriTemplate(MILESTONE_URI).expand(parameters);

			logger.log(module, "Looking up milestone from %s…", milestoneUri);

			List<GitHubMilestone> exchange = operations.exchange(MILESTONE_URI, HttpMethod.GET,
					new HttpEntity<>(getAuthenticationHeaders()), MILESTONES_TYPE, parameters).getBody();

			GitHubMilestone milestone = null;

			for (GitHubMilestone candidate : exchange) {
				if (candidate.getTitle().contains(module.getShortVersionString())) {
					milestone = candidate;
				}
			}

			if (milestone != null) {
				logger.log(module, "Found milestone %s.", milestone);
				return Optional.of(milestone);
			}
		}

		return Optional.empty();
	}

	private HttpHeaders getAuthenticationHeaders() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", properties.getHttpCredentials().toString());

		return headers;
	}

	private Map<String, Object> newUrlTemplateVariables() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("githubBaseUrl", gitubBaseUrl);
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
