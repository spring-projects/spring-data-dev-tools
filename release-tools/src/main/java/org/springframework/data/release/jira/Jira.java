/*
 * Copyright 2013 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.release.Application;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriTemplate;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
class Jira implements JiraConnector {

	private static final String JIRA_HOST = "https://jira.spring.io";
	private static final String BASE_URI = "/rest/api/2";
	private static final String SEARCH_TEMPLATE = JIRA_HOST + BASE_URI
			+ "/search?jql={jql}&fields={fields}&startAt={startAt}";

	private final RestOperations operations;
	private final Logger logger;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#flushTickets()
	 */
	@Override
	@CacheEvict(value = "tickets", allEntries = true)
	public void reset() {}

	@Cacheable("release-tickets")
	public Ticket getReleaseTicketFor(ModuleIteration iteration) {

		JqlQuery query = JqlQuery.from(iteration).and("summary ~ \"Release\"");

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("jql", query);
		parameters.put("fields", "summary");
		parameters.put("startAt", 0);

		JiraIssues issues = operations.exchange(SEARCH_TEMPLATE, HttpMethod.GET, null, JiraIssues.class, parameters)
				.getBody();

		if (issues.getIssues().isEmpty()) {
			throw new IllegalStateException(String.format("Did not find a release ticket for %s!", iteration));
		}

		JiraIssue issue = issues.getIssues().get(0);

		return new Ticket(issue.getKey(), issue.getFields().getSummary());
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
		int startAt = 0;
		List<Ticket> tickets = new ArrayList<>();
		JiraIssues issues = null;

		if (credentials != null) {

			query = query.and("assignee = currentUser()");

			headers.set("Authorization", String.format("Basic %s", credentials.asBase64()));

			logger.log(iteration, "Retrieving tickets (for user %s)…", credentials.getUsername());
		} else {
			logger.log(iteration, "Retrieving tickets…");
		}

		query = query.orderBy("updatedDate DESC");

		do {

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("jql", query);
			parameters.put("fields", "summary,fixVersions");
			parameters.put("startAt", startAt);

			issues = operations
					.exchange(SEARCH_TEMPLATE, HttpMethod.GET, new HttpEntity<>(headers), JiraIssues.class, parameters).getBody();

			logger.log(iteration, "Got tickets %s to %s of %s.", startAt, issues.getNextStartAt(), issues.getTotal());

			for (JiraIssue issue : issues) {
				if (!issue.wasBackportedFrom(iteration.getTrain())) {
					tickets.add(new Ticket(issue.getKey(), issue.getFields().getSummary()));
				}
			}

			startAt = issues.getNextStartAt();

		} while (issues.hasMoreResults());

		return new Tickets(Collections.unmodifiableList(tickets), issues.getTotal());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#verifyBeforeRelease(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration)
	 */
	@Override
	public void verifyBeforeRelease(TrainIteration iteration) {

		// for each module

		// - make sure only one ticket is open
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

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("jql", JqlQuery.from(module));
		parameters.put("fields", "summary,fixVersions");
		parameters.put("startAt", 0);

		URI searchUri = new UriTemplate(SEARCH_TEMPLATE).expand(parameters);

		logger.log(module, "Looking up JIRA issues from %s…", searchUri);

		JiraIssues issues = operations.getForObject(searchUri, JiraIssues.class);
		List<Ticket> tickets = new ArrayList<>();

		for (JiraIssue issue : issues) {
			tickets.add(new Ticket(issue.getKey(), issue.getFields().getSummary()));
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
}
