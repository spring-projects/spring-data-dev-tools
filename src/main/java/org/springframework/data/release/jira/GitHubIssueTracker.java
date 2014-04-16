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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.git.GitServer;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
class GitHubIssueTracker implements IssueTracker {

	private static final String MILESTONE_URI = "https://api.github.com/repos/spring-projects/{repoName}/milestones?state={state}";
	private static final String URI_TEMPLATE = "https://api.github.com/repos/spring-projects/{repoName}/issues?milestone={id}&state=all";

	private static final ParameterizedTypeReference<List<GitHubMilestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<GitHubMilestone>>() {};
	private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUES_TYPE = new ParameterizedTypeReference<List<GitHubIssue>>() {};

	private final RestOperations operations;
	private final Logger logger;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getChangelogFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public Changelog getChangelogFor(ModuleIteration module) {

		String repositoryName = new GitProject(module.getProject(), new GitServer()).getRepositoryName();

		GitHubMilestone milestone = findMilestone(module, repositoryName);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());

		List<GitHubIssue> issues = operations.exchange(URI_TEMPLATE, HttpMethod.GET, null, ISSUES_TYPE, parameters)
				.getBody();
		List<Ticket> tickets = new ArrayList<>(issues.size());

		for (GitHubIssue issue : issues) {
			tickets.add(new Ticket("#" + issue.getNumber(), issue.getTitle()));
		}

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

	private GitHubMilestone findMilestone(ModuleIteration module, String repositoryName) {

		for (String state : Arrays.asList("close", "open")) {

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("repoName", repositoryName);
			parameters.put("state", state);

			URI milestoneUri = new UriTemplate(MILESTONE_URI).expand(parameters);

			logger.log(module, "Looking up milestone from %s…", milestoneUri);

			List<GitHubMilestone> exchange = operations.exchange(MILESTONE_URI, HttpMethod.GET, null, MILESTONES_TYPE,
					parameters).getBody();

			GitHubMilestone milestone = null;

			for (GitHubMilestone candidate : exchange) {
				if (candidate.getTitle().contains(module.getVersionString())) {
					milestone = candidate;
				}
			}

			if (milestone != null) {
				logger.log(module, "Found milestone %s.", milestone);
				return milestone;
			}
		}

		throw new IllegalStateException(String.format("No milestone found containing %s!", module.getVersionString()));
	}

	public static void main(String[] args) {

		try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"META-INF/spring/spring-shell-plugin.xml")) {

			GitHubIssueTracker tracker = context.getBean(GitHubIssueTracker.class);

			TrainIteration iteration = new TrainIteration(ReleaseTrains.CODD, Iteration.SR2);
			Changelog changelog = tracker.getChangelogFor(iteration.getModule("Build"));

			System.out.println(changelog);
		}
	}
}
