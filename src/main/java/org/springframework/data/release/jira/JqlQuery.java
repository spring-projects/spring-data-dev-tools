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
import java.util.List;

import lombok.Value;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@Value
class JqlQuery {

	private static final String PROJECT_VERSION_TEMPLATE = "project = %s AND fixVersion = \"%s\"";

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

	public static JqlQuery from(Train train, Iteration iteration) {

		List<String> parts = new ArrayList<>();

		for (ModuleIteration module : train.getModuleIterations(iteration)) {

			if (ReleaseTrains.BUILD.equals(module.getProject())) {
				continue;
			}

			JiraVersion version = new JiraVersion(module);
			parts.add(String.format(PROJECT_VERSION_TEMPLATE, module.getProjectKey(), version));
		}

		return new JqlQuery(StringUtils.collectionToDelimitedString(parts, " OR "));
	}

	@Override
	public String toString() {
		return query;
	}
}
