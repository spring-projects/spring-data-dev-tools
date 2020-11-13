/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.dependency;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Projects;

/**
 * Integration tests for {@link DependencyOperations}.
 *
 * @author Mark Paluch
 */
@Disabled
class DependencyOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired GitOperations git;
	@Autowired DependencyOperations operations;

	@BeforeAll
	static void beforeAll() {
		try {
			URL url = new URL("https://repo1.maven.org");
			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
			urlConnection.getInputStream().close();
		} catch (IOException e) {
			assumeTrue("Test requires connectivity to Maven: " + e.toString(), false);
		}
	}

	@Test
	void shouldDiscoverDependencyVersions() {
		assertThat(operations.getAvailableVersions(Dependencies.PROJECT_REACTOR)).isNotEmpty();
	}

	@Test
	void shouldReportExistingDependencyVersions() {
		assertThat(operations.getCurrentDependencies(Projects.BUILD)).isNotEmpty();
	}

	@Test
	void shouldReportExistingOptionalDependencies() {

		// git.checkout(ReleaseTrains.MOORE);

		assertThat(operations.getCurrentDependencies(Projects.CASSANDRA)).hasSize(1);
		assertThat(operations.getCurrentDependencies(Projects.MONGO_DB)).hasSize(1);
		assertThat(operations.getCurrentDependencies(Projects.NEO4J)).hasSize(1);
	}

	@Test
	void getUpgradeProposals() {
		System.out.println(operations.getDependencyUpgradeProposals(Projects.BUILD, Iteration.M1));
	}
}
