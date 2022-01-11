/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.git;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;

/**
 * Unit tests for {@link GitProject}.
 *
 * @author Oliver Gierke
 */
class GitProjectUnitTests {

	@Test
	void buildsGitHubRepositoryUriCorrectly() {

		Train codd = ReleaseTrains.CODD;
		GitServer server = new GitServer();
		Module module = codd.getModule(Projects.COMMONS);
		Project project = module.getProject();

		GitProject gitProject = new GitProject(project, server);

		String projectUri = gitProject.getProjectUri();

		assertThat(projectUri).startsWith(server.getUri());
		assertThat(projectUri).endsWith("spring-data-commons");
	}
}
