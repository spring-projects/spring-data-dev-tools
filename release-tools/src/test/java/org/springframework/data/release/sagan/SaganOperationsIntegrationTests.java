/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.release.sagan;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Integration tests for {@link SaganOperations}.
 * 
 * @author Oliver Gierke
 */
public class SaganOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired SaganOperations sagan;
	@Autowired SaganClient client;

	@Test
	public void detectVersionsToUpdate() {

		sagan.findVersions(ReleaseTrains.LOVELACE, ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER)
				.forEach((project, versions) -> {

					System.out.println(project.getName());
					System.out.println("-----");

					versions.forEach(version -> {

						String output = version.toString();
						System.out.println(versions.isMainVersion(version) ? output.concat(" (main release)") : output);
					});

					System.out.println();
				});
	}

	@Test
	public void updateVersions() {
		sagan.updateProjectMetadata(ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER);
	}

	@Test
	public void updateJpa() {

		MaintainedVersions versions = sagan.findVersions(ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER)
				.get(Projects.JPA);

		System.out.println(versions);

		client.updateProjectMetadata(Projects.JPA, versions);
	}

	@Test
	public void getJpa() {
		System.out.println(client.getProjectMetadata(Projects.JPA));
	}

	@Test
	public void updateBuild() {

		MaintainedVersions versions = sagan.findVersions(ReleaseTrains.KAY, ReleaseTrains.INGALLS, ReleaseTrains.HOPPER)
				.get(Projects.BUILD);

		client.updateProjectMetadata(Projects.BUILD, versions);
	}
}
