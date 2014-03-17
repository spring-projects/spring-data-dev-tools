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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Train;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
class RestJiraConnector implements JiraConnector {

	protected final Logger LOGGER = Logger.getLogger(getClass().getName());

	private static final String JIRA_HOST = "https://jira.spring.io";
	private static final String BASE_URI = "/rest/api/2";
	private static final String SEARCH_TEMPLATE = JIRA_HOST + BASE_URI
			+ "/search?jql={jql}&fields={fields}&startAt={startAt}";

	private final RestOperations operations;

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
	 * @see org.springframework.data.release.jira.JiraConnector#getTicketsFor(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	@Cacheable("tickets")
	public Tickets getTicketsFor(Train train, Iteration iteration, Credentials credentials) {

		JqlQuery query = JqlQuery.from(train, iteration);

		HttpHeaders headers = new HttpHeaders();
		int startAt = 0;
		List<Ticket> tickets = new ArrayList<>();
		JiraIssues issues = null;

		if (credentials != null) {

			query = query.and("assignee = currentUser()");

			headers.set("Authorization", String.format("Basic %s", credentials.asBase64()));

			LOGGER.info(String.format("Retrieving tickets for %s %s (for user %s).", train.getName(), iteration.getName(),
					credentials.getUsername()));
		} else {
			LOGGER.info(String.format("Retrieving tickets for %s %s.", train.getName(), iteration.getName()));
		}

		query = query.orderBy("updatedDate DESC");

		do {

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("jql", query);
			parameters.put("fields", "summary,fixVersions");
			parameters.put("startAt", startAt);

			issues = operations.exchange(SEARCH_TEMPLATE, HttpMethod.GET, new HttpEntity<>(headers), JiraIssues.class,
					parameters).getBody();

			LOGGER.info(String.format("Got tickets %s to %s of %s.", startAt, issues.getNextStartAt(), issues.getTotal()));

			for (JiraIssue issue : issues) {
				if (!issue.wasBackportedFrom(train)) {
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
	public void verifyBeforeRelease(Train train, Iteration iteration) {

		// for each module

		// - make sure only one ticket is open
	}

	/*
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#closeIteration(org.springframework.data.release.model.Train, org.springframework.data.release.model.Iteration, org.springframework.data.release.jira.Credentials)
	 */
	@Override
	public void closeIteration(Train train, Iteration iteration, Credentials credentials) {

		// for each module

		// - close all tickets
		// -- make sure only one ticket is open
		// -- resolve open ticket
		// -- close tickets

		// - mark version as releases
		// - if no next version exists, create
	}
}
