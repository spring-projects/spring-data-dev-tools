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
package org.springframework.data.release.issues.jira;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.release.issues.jira.JiraVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Unit tests for {@link JiraVersion}.
 *
 * @author Oliver Gierke
 */
public class JiraVersionUnitTests {

	@Test
	public void rendersJiraGaVersionCorrectly() {

		assertIterationVersion(Iteration.M1, "1.8 M1 (Dijkstra)");
		assertIterationVersion(Iteration.RC1, "1.8 RC1 (Dijkstra)");
		assertIterationVersion(Iteration.GA, "1.8 GA (Dijkstra)");

		assertIterationVersion(Iteration.SR1, "1.8.1 (Dijkstra SR1)");
		assertIterationVersion(Iteration.SR2, "1.8.2 (Dijkstra SR2)");
		assertIterationVersion(Iteration.SR3, "1.8.3 (Dijkstra SR3)");
		assertIterationVersion(Iteration.SR4, "1.8.4 (Dijkstra SR4)");
	}

	@Test
	public void usesCustomModuleIterationStartVersion() {

		ModuleIteration module = ReleaseTrains.DIJKSTRA.getModuleIteration(Iteration.M1, "Elasticsearch");

		JiraVersion version = new JiraVersion(module);
		assertThat(version.toString(), is("1.0 M1 (Dijkstra)"));
	}

	@Test
	public void doesNotUseCustomIterationOnNonFirstiterations() {

		ModuleIteration module = ReleaseTrains.DIJKSTRA.getModuleIteration(Iteration.RC1, "Elasticsearch");

		JiraVersion version = new JiraVersion(module);
		assertThat(version.toString(), is("1.0 RC1 (Dijkstra)"));
	}

	@Test
	public void rendersDescriptionCorrectly() {

		ModuleIteration module = ReleaseTrains.DIJKSTRA.getModuleIteration(Iteration.M1, "Elasticsearch");

		JiraVersion version = new JiraVersion(module);
		assertThat(version.getDescription(), is("Dijkstra M2"));
	}

	private void assertIterationVersion(Iteration iteration, String expected) {

		ModuleIteration module = ReleaseTrains.DIJKSTRA.getModuleIteration(iteration, "Commons");

		JiraVersion version = new JiraVersion(module);
		assertThat(version.toString(), is(expected));
	}
}
