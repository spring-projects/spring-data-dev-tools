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

import java.util.Locale;

import org.springframework.data.release.build.MavenArtifact;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Projects;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Simple value object to create payloads to update project metadata in Sagan.
 *
 * @author Oliver Gierke
 */
@JsonInclude(Include.NON_NULL)
class ProjectMetadata {

	private static String DOCS_BASE = "https://docs.spring.io/spring-data/%s/docs/{version}";
	private static String DOCS = DOCS_BASE.concat("/reference/html/");
	private static String JAVADOC = DOCS_BASE.concat("/api");

	private final MaintainedVersions versions;
	private final MaintainedVersion version;
	private final MavenArtifact artifact;

	/**
	 * Creates a new {@link ProjectMetadata} instace from the given {@link MaintainedVersion} in the context of the
	 * {@link MaintainedVersions}.
	 * 
	 * @param version must not be {@literal null}.
	 * @param versions must not be {@literal null}.
	 */
	public ProjectMetadata(MaintainedVersion version, MaintainedVersions versions) {

		Assert.notNull(version, "MaintainedVersion must not be null!");
		Assert.notNull(versions, "MaintainedVersions must not be null!");

		this.version = version;
		this.versions = versions;
		this.artifact = new MavenArtifact(version.getProject(), version.getVersion());
	}

	/**
	 * Returns the release status used on Sagan.
	 * 
	 * @return
	 */
	public String getReleaseStatus() {

		ArtifactVersion artifactVersion = version.getVersion();

		if (artifactVersion.isReleaseVersion()) {
			return "GENERAL_AVAILABILITY";
		}

		if (artifactVersion.isMilestoneVersion()) {
			return "PRERELEASE";
		}

		if (artifactVersion.isSnapshotVersion()) {
			return "SNAPSHOT";
		}

		throw new IllegalStateException();
	}

	/**
	 * Returns the group identifier of the release.
	 * 
	 * @return
	 */
	public String getGroupId() {
		return artifact.getGroupId();
	}

	/**
	 * Returns the artifact identifier.
	 * 
	 * @return
	 */
	public String getArtifactId() {

		if (Projects.BUILD.equals(version.getProject())) {
			return "spring-data-releasetrain";
		}

		return artifact.getArtifactId();
	}

	/**
	 * Returns whether the version is the most current one.
	 * 
	 * @return
	 */
	public Boolean getCurrent() {
		return versions.isMainVersion(version) ? true : null;
	}

	/**
	 * Returns the reference documentation URL for non-snapshot versions and not the build project.
	 * 
	 * @return
	 */
	public String getRefDocUrl() {

		return version.getVersion().isSnapshotVersion() || Projects.BUILD.equals(version.getProject()) //
				? "" //
				: String.format(DOCS, version.getProject().getName().toLowerCase(Locale.US));
	}

	/**
	 * Returns the JavaDoc URL for non-snapshot versions and not the build project.
	 * 
	 * @return
	 */
	public String getApiDocUrl() {

		return version.getVersion().isSnapshotVersion() || Projects.BUILD.equals(version.getProject()) //
				? "" //
				: String.format(JAVADOC, version.getProject().getName().toLowerCase(Locale.US));
	}

	public Repository getRepository() {
		return new Repository();
	}

	/**
	 * Return the version to use. For the build project thats the release train name, for everything else the artifact
	 * version.
	 * 
	 * @return
	 */
	public String getVersion() {

		if (Projects.BUILD.equals(version.getProject())) {
			return String.format("%s-%s", version.getTrain().getName(), version.getVersion().getReleaseTrainSuffix());
		}

		return version.getVersion().toString();
	}

	public class Repository {

		public String getId() {
			return artifact.getRepository().getId();
		}

		public boolean isSnapshotsEnabled() {
			return version.getVersion().isSnapshotVersion();
		}

		public String getUrl() {
			return artifact.getRepository().getUrl();
		}

		public String getName() {

			String result = "Spring ";

			if (version.getVersion().isMilestoneVersion()) {
				return result.concat("Milestones");
			}

			if (version.getVersion().isReleaseVersion()) {
				return result.concat("Releases");
			}

			if (isSnapshotsEnabled()) {
				return result.concat("Snapshots");
			}

			throw new IllegalStateException();
		}
	}
}
