/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.data.release.sagan;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Integration tests for {@link SaganOperations}.
 *
 * @author Oliver Gierke
 */
@Disabled("I will write to production systems")
class SaganOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired SaganOperations sagan;
	@Autowired SaganClient client;

	@Test
	void detectVersionsToUpdate() {

		sagan.findVersions(ReleaseTrains.OCKHAM, ReleaseTrains.NEUMANN, ReleaseTrains.MOORE, ReleaseTrains.LOVELACE)
				.forEach((project, versions) -> {

					System.out.println(project.getName());
					System.out.println("-----");

					versions.forEach(version -> {

						String output = version.toString();
						System.out.println(versions.isMainVersion(version) ? output.concat(" (main release)") : output);
					});
				});
	}

	@Test
	void updateVersions() {
		sagan.updateProjectMetadata(ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER);
	}

	@Test
	void findVersions() {
		Map<Project, MaintainedVersions> versions = sagan.findVersions(ReleaseTrains.LOVELACE, ReleaseTrains.MOORE,
				ReleaseTrains.NEUMANN);

		List<MaintainedVersion> maintainedVersions = versions.get(Projects.ELASTICSEARCH)
				.filter(it -> it.getVersion().isReleaseVersion()).toList();

		assertThat(maintainedVersions).isNotEmpty();
		assertThat(maintainedVersions.get(0).getGenerationInception()).isNotNull();
		assertThat(maintainedVersions.get(0).getReleaseDate()).isNotNull();
	}

	@Test
	void updateJpa() {

		MaintainedVersions versions = sagan.findVersions(ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER)
				.get(Projects.JPA);

		System.out.println(versions);

		client.updateProjectMetadata(Projects.JPA, versions);
	}

	@Test
	void getJpa() {
		System.out.println(client.getProjectMetadata(Projects.JPA));
	}

	@Test
	void updateBuild() {

		MaintainedVersions versions = sagan.findVersions(ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER)
				.get(Projects.BUILD);

		client.updateProjectMetadata(Projects.BUILD, versions);
	}
}
