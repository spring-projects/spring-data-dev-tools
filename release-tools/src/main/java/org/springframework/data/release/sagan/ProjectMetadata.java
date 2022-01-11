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

import org.springframework.data.release.model.DocumentationMetadata;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple value object to create payloads to update project metadata in Sagan.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@JsonInclude(Include.NON_NULL)
class ProjectMetadata {

	private final MaintainedVersion version;
	private final MaintainedVersions versions;
	private final DocumentationMetadata documentation;

	/**
	 * Creates a new {@link ProjectMetadata} instance from the given {@link MaintainedVersion}.
	 *
	 * @param version must not be {@literal null}.
	 * @param versions must not be {@literal null}.
	 */
	public ProjectMetadata(MaintainedVersion version, MaintainedVersions versions) {

		Assert.notNull(version, "MaintainedVersion must not be null!");

		this.version = version;
		this.versions = versions;
		this.documentation = DocumentationMetadata.of(version.getProject(), version.getVersion(),
				versions.isMainVersion(version));
	}

	/**
	 * Returns the reference documentation URL for non-snapshot versions and not the build project.
	 *
	 * @return
	 */
	public String getReferenceDocUrl() {
		return documentation.getReferenceDocUrl();
	}

	/**
	 * Returns whether the version is the most current one.
	 *
	 * @return
	 */
	@JsonProperty("isCurrent")
	public boolean getCurrent() {
		return versions.isMainVersion(version);
	}

	/**
	 * Returns the JavaDoc URL for non-snapshot versions and not the build project.
	 *
	 * @return
	 */
	public String getApiDocUrl() {
		return documentation.getApiDocUrl();
	}

	/**
	 * Return the version to use. For the build project that's the release train name, for everything else the artifact
	 * version.
	 *
	 * @return
	 */
	public String getVersion() {
		return documentation.getVersionOrTrainName(version.getTrain());
	}
}
