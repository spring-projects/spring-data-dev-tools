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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minidev.json.JSONArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import com.jayway.jsonpath.JsonPath;

/**
 * Sagan client to interact with the Sagan instance defined through {@link SaganProperties}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class DefaultSaganClient implements SaganClient {

	RestOperations operations;
	SaganProperties properties;
	Logger logger;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#getProjectMetadata(org.springframework.data.release.sagan.MaintainedVersion)
	 */
	@Override
	public String getProjectMetadata(MaintainedVersion version) {

		URI resource = properties.getProjectMetadataResource(version);

		logger.log(version.getProject(), "Getting project metadata for version %s from %s…", version.getVersion(),
				resource);

		return operations.getForObject(resource, String.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#getProjectMetadata(org.springframework.data.release.model.Project)
	 */
	@Override
	public String getProjectMetadata(Project project) {

		URI resource = properties.getProjectReleasesResource(project);

		logger.log(project, "Getting project releases from %s…", resource);

		return operations.getForObject(resource, String.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#updateProjectMetadata(org.springframework.data.release.model.Project, java.util.List)
	 */
	@Override
	public void updateProjectMetadata(Project project, MaintainedVersions versions) {

		URI resource = properties.getProjectReleasesResource(project);

		String versionsString = versions.stream()//
				.map(MaintainedVersion::getVersion)//
				.map(Object::toString) //
				.collect(Collectors.joining(", "));
		List<String> versionsToRetain = versions.stream() //
				.map(version -> new ProjectMetadata(version, versions)).map(ProjectMetadata::getVersion)
				.collect(Collectors.toList());
		List<String> versionsInSagan = new ArrayList<>();

		logger.log(project, "Updating project version to %s via %s…", versionsString, resource);

		// Delete all existing versions first
		deleteExistingVersions(project, versionsToRetain, versionsInSagan);

		logger.log(project, "Writing project versions %s.", versionsString);

		// Write new ones
		createVersions(versions, resource, versionsInSagan);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#verifyAuthentication()
	 */
	@Override
	public void verifyAuthentication() {

		URI resource = properties.getProjectReleasesResource(Projects.BUILD);

		logger.log("Sagan", "Verifying Sagan Authentication…");

		ResponseEntity<String> entity = operations.getForEntity(resource, String.class);

		if (!entity.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException("Cannot access Jira user profile");
		}

		logger.log("Sagan", "Authentication verified!");
	}

	private void createVersions(MaintainedVersions versions, URI resource, List<String> versionsInSagan) {

		versions.stream() //
				.map(it -> new ProjectMetadata(it, versions)) //
				.filter(version -> !versionsInSagan.contains(version.getVersion())) //
				.forEach(payload -> operations.postForObject(resource, payload, String.class));
	}

	private void deleteExistingVersions(Project project, List<String> versionsToRetain, List<String> versionsInSagan) {

		Arrays.stream(JsonPath.compile("$..version").<JSONArray> read(getProjectMetadata(project)).toArray())//
				.map(Object::toString) //
				.peek(versionsInSagan::add) //
				.filter(version -> !versionsToRetain.contains(version)) //
				.map(version -> properties.getProjectReleaseResource(project, version))//
				.peek(uri -> logger.log(project, "Deleting existing project version at %s…", uri)) //
				.forEach(operations::delete);
	}
}
