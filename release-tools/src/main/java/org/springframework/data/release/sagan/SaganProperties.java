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

import lombok.Setter;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
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

	private static String SAGAN_BASE = "https://spring.io/api/";
	private static String SAGAN_RELEASES = SAGAN_BASE.concat("projects/{project}/releases");
	private static String SAGAN_GENERATIONS = SAGAN_BASE.concat("projects/{project}/generations");
	private static String SAGAN_RELEASE_VERSION = SAGAN_RELEASES.concat("/{version}");

	@Setter String key;

	/**
	 * Returns the URI to the resource exposing the project releases for the given {@link Project}.
	 *
	 * @param project must not be {@literal null}.
	 * @return
	 */
	URI getProjectReleasesResource(Project project) {

		Assert.notNull(project, "Project must not be null!");

		return new UriTemplate(SAGAN_RELEASES).expand(getProjectPathSegment(project));
	}

	/**
	 * Returns the URI to the resource exposing the project generations for the given {@link Project}.
	 *
	 * @param project must not be {@literal null}.
	 * @return
	 */
	URI getProjectGenerationsResource(Project project) {

		Assert.notNull(project, "Project must not be null!");

		return new UriTemplate(SAGAN_GENERATIONS).expand(getProjectPathSegment(project));
	}

	/**
	 * Returns the URI to the resource exposing the project version for the given {@link Project} and version
	 * {@link String}.
	 *
	 * @param project must not be {@literal null}.
	 * @param version must not be {@literal null}.
	 * @return
	 */
	URI getProjectReleaseResource(Project project, String version) {

		Assert.notNull(project, "Project  must not be null!");
		Assert.hasText(version, "Version must not be null!");

		return new UriTemplate(SAGAN_RELEASE_VERSION).expand(getProjectPathSegment(project), version);
	}

	/**
	 * Returns the {@link URI} to the resource exposing the project version for the given {@link MaintainedVersion}.
	 *
	 * @param version must not be {@literal null}.
	 * @return
	 */
	URI getProjectMetadataResource(MaintainedVersion version) {
		return getProjectReleaseResource(version.getProject(), version.getVersion().toString());
	}

	private static String getProjectPathSegment(Project project) {

		if (Projects.BUILD.equals(project)) {
			return "spring-data";
		}

		if (Projects.RELATIONAL.equals(project) || Projects.JDBC.equals(project)) {
			return "spring-data-jdbc";
		}

		return project.getFolderName();
	}
}
