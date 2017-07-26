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

import lombok.Setter;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.release.model.Password;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.utils.HttpBasicCredentials;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriTemplate;

/**
 * Configuration properties for the Sagan instance to talk to.
 * 
 * @author Oliver Gierke
 */
@Component
@ConfigurationProperties(prefix = "sagan")
class SaganProperties {

	private static String SAGAN_PROJECT_METADATA = "https://spring.io/project_metadata/{project}/releases";
	private static String SAGAN_PROJECT_VERSION_METADATA = SAGAN_PROJECT_METADATA.concat("/{version}");

	@Setter String key;

	/**
	 * Returns the {@link HttpBasicCredentials} to be used when talking to the server.
	 * 
	 * @return
	 */
	HttpBasicCredentials getCredentials() {
		return new HttpBasicCredentials(key, Password.NONE);
	}

	/**
	 * Returns the URI to the resource exposing the project metadata for the given {@link Project}.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	URI getProjectMetadataResource(Project project) {

		Assert.notNull(project, "Project  must not be null!");

		return new UriTemplate(SAGAN_PROJECT_METADATA).expand(getProjectPathSegment(project));
	}

	/**
	 * Returns the URI to the resource exposing the project metadata for the given {@link Project} and version
	 * {@link String}.
	 * 
	 * @param project must not be {@literal null}.
	 * @param version must not be {@literal null}.
	 * @return
	 */
	URI getProjectMetadataResource(Project project, String version) {

		Assert.notNull(project, "Project  must not be null!");
		Assert.hasText(version, "Version must not be null!");

		return new UriTemplate(SAGAN_PROJECT_VERSION_METADATA).expand(getProjectPathSegment(project), version);
	}

	/**
	 * Returns the {@link URI} to the resource exposing the project metadata for the given {@link MaintainedVersion}.
	 * 
	 * @param version must not be {@literal null}.
	 * @return
	 */
	URI getProjectMetadataResource(MaintainedVersion version) {
		return getProjectMetadataResource(version.getProject(), version.getVersion().toString());
	}

	private static String getProjectPathSegment(Project project) {
		return Projects.BUILD.equals(project) ? "spring-data" : project.getFolderName();
	}
}
