/*
 * Copyright 2014 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.git.GitProperties;
import org.springframework.data.release.git.GitServer;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
class GitHubIssueTracker implements GitHubConnector {

	private static final String MILESTONE_URI = "https://api.github.com/repos/spring-projects/{repoName}/milestones?state={state}";
	private static final String ISSUES_BY_MILESTONE_URI_TEMPLATE = "https://api.github.com/repos/spring-projects/{repoName}/issues?milestone={id}&state=all";
	private static final String ISSUE_BY_ID_URI_TEMPLATE = "https://api.github.com/repos/spring-projects/{repoName}/issues/{id}";

	private static final ParameterizedTypeReference<List<GitHubMilestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<GitHubMilestone>>() {};
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
	@CacheEvict(value = "tickets", allEntries = true)
	public void reset() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getReleaseTicketFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("release-tickets")
	public Ticket getReleaseTicketFor(ModuleIteration module) {

		return getIssuesFor(module).stream().//
				filter(issue -> issue.isReleaseTicket(module)).//
				findFirst().//
				map(issue -> toTicket(issue)).//
				orElseThrow(
						() -> new IllegalArgumentException(String.format("Could not find a release ticket for %s!", module)));
	}

	private Ticket toTicket(GitHubIssue issue) {
		return new Ticket(issue.getId(), issue.getTitle(), new GithubTicketStatus(issue.getState()));
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see IssueTracker#findTickets(Project, Collection)
	 */
	@Override
	@Cacheable("tickets")
	public Collection<Ticket> findTickets(Project project, Collection<String> ticketIds) {

		String repositoryName = new GitProject(project, new GitServer()).getRepositoryName();
		List<Ticket> tickets = new ArrayList<>();
		for (String ticketId : ticketIds) {

			Map<String, Object> parameters = new HashMap<>();
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

		List<Ticket> tickets = getIssuesFor(module).stream().//
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

	private List<GitHubIssue> getIssuesFor(ModuleIteration module) {

		String repositoryName = new GitProject(module.getProject(), new GitServer()).getRepositoryName();

		GitHubMilestone milestone = findMilestone(module, repositoryName);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());

		return operations.exchange(ISSUES_BY_MILESTONE_URI_TEMPLATE, HttpMethod.GET,
				new HttpEntity<>(getAuthenticationHeaders()), ISSUES_TYPE, parameters).getBody();
	}

	private GitHubMilestone findMilestone(ModuleIteration module, String repositoryName) {

		for (String state : Arrays.asList("close", "open")) {

			Map<String, Object> parameters = new HashMap<>();
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
				return milestone;
			}
		}

		throw new IllegalStateException(String.format("No milestone found containing %s!", module.getShortVersionString()));
	}

	private HttpHeaders getAuthenticationHeaders() {

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", properties.getHttpCredentials().toString());

		return headers;
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
