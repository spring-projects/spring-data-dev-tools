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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minidev.json.JSONArray;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;
import org.springframework.web.client.RestOperations;

import com.jayway.jsonpath.JsonPath;

/**
 * Sagan client to interact with the Sagan instance defined through {@link SaganProperties}.
 * 
 * @author Oliver Gierke
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

		URI resource = properties.getProjectMetadataResource(project);

		logger.log(project, "Getting project metadata from %s…", resource);

		return operations.getForObject(resource, String.class);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#updateProjectMetadata(org.springframework.data.release.model.Project, java.util.List)
	 */
	@Override
	public void updateProjectMetadata(Project project, MaintainedVersions versions) {

		URI resource = properties.getProjectMetadataResource(project);

		String versionsString = versions.stream()//
				.map(MaintainedVersion::getVersion)//
				.map(Object::toString) //
				.collect(Collectors.joining(", "));

		logger.log(project, "Updating project metadata to %s via %s…", versionsString, resource);

		// Delete all existing versions first
		Arrays.stream(JsonPath.compile("$..version").<JSONArray> read(getProjectMetadata(project)).toArray())//
				.map(version -> properties.getProjectMetadataResource(project, version.toString()))//
				.peek(uri -> logger.log(project, "Deleting existing project metadata at %s…", uri)) //
				.forEach(uri -> operations.delete(uri));

		logger.log(project, "Writing project metadata for versions %s!", versionsString);

		// Write new ones
		List<ProjectMetadata> payload = versions.stream() //
				.map(version -> new ProjectMetadata(version, versions)) //
				.collect(Collectors.toList());

		operations.put(resource, payload);
	}
}
