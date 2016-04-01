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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.util.StringUtils;

import lombok.Value;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
class JqlQuery {

	private static final String PROJECT_VERSION_TEMPLATE = "(project = %s AND fixVersion = \"%s\" )";
	private static final String ISSUE_KEY_IN_TEMPLATE = "issueKey in (%s)";

	private final String query;

	public JqlQuery and(String clause) {
		return new JqlQuery(String.format("(%s) AND %s", query, clause));
	}

	public JqlQuery orderBy(String orderBy) {
		return new JqlQuery(String.format("%s ORDER BY %s", query, orderBy));
	}

	public static JqlQuery from(ModuleIteration iteration) {

		JiraVersion version = new JiraVersion(iteration);

		return new JqlQuery(String.format(PROJECT_VERSION_TEMPLATE, iteration.getProjectKey(), version));
	}

	public static JqlQuery from(Collection<String> ticketIds) {

		String joinedTicketIds = ticketIds.stream().collect(Collectors.joining(", "));

		return new JqlQuery(String.format(ISSUE_KEY_IN_TEMPLATE, joinedTicketIds));
	}

	public static JqlQuery from(Stream<ModuleIteration> stream) {

		List<String> parts = new ArrayList<>();

		stream.forEach(moduleIteration -> {
			JiraVersion version = new JiraVersion(moduleIteration);
			parts.add(String.format(PROJECT_VERSION_TEMPLATE, moduleIteration.getProjectKey(), version));
		});

		return new JqlQuery(StringUtils.collectionToDelimitedString(parts, " OR "));
	}

	@Override
	public String toString() {
		return query;
	}
}
