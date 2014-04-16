/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.release.model;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import org.springframework.util.Assert;

/**
 * Value object to represent version of a particular artifact.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ArtifactVersion implements Comparable<ArtifactVersion> {

	private static final String RELEASE_SUFFIX = "RELEASE";
	private static final String MILESTONE_SUFFIX = "M\\d|RC\\d";
	private static final String SNAPSHOT_SUFFIX = "BUILD-SNAPSHOT";

	private static final String VALID_SUFFIX = String.format("%s|%s|%s", RELEASE_SUFFIX, MILESTONE_SUFFIX,
			SNAPSHOT_SUFFIX);

	private final Version version;
	private final String suffix;

	/**
	 * Creates a new {@link ArtifactVersion} from the given logical {@link Version}.
	 * 
	 * @param version must not be {@literal null}.
	 */
	public ArtifactVersion(Version version) {

		Assert.notNull(version, "Version must not be null!");

		this.version = version;
		this.suffix = RELEASE_SUFFIX;
	}

	/**
	 * Parses the given {@link String} into an {@link ArtifactVersion}.
	 * 
	 * @param source must not be {@literal null} or empty.
	 * @return
	 */
	public static ArtifactVersion parse(String source) {

		Assert.hasText(source, "Version source must not be null or empty!");

		int suffixStart = source.lastIndexOf('.');

		Version version = Version.parse(source.substring(0, suffixStart));
		String suffix = source.substring(suffixStart + 1);

		Assert.isTrue(suffix.matches(VALID_SUFFIX), "Invalid version suffix!");

		return new ArtifactVersion(version, suffix);
	}

	/**
	 * Creates a new {@link ArtifactVersion} from the given {@link IterationVersion}.
	 * 
	 * @param iterationVersion must not be {@literal null}.
	 * @return
	 */
	public static ArtifactVersion from(IterationVersion iterationVersion) {

		Assert.notNull(iterationVersion, "IterationVersion must not be null!");

		Version version = iterationVersion.getVersion();
		String iterationName = iterationVersion.getIteration().getName();

		if (iterationName.equals("GA")) {
			return new ArtifactVersion(version, RELEASE_SUFFIX);
		}

		if (iterationName.startsWith("SR")) {
			int bugfixDigits = Integer.parseInt(iterationName.substring(2, iterationName.length()));
			return new ArtifactVersion(version.withBugfix(bugfixDigits), RELEASE_SUFFIX);
		}

		return new ArtifactVersion(version, iterationName);
	}

	/**
	 * Returns the release version for the current one.
	 * 
	 * @return
	 */
	public ArtifactVersion getReleaseVersion() {
		return new ArtifactVersion(version, RELEASE_SUFFIX);
	}

	/**
	 * Returns the snapshot version of the current one.
	 * 
	 * @return
	 */
	public ArtifactVersion getSnapshotVersion() {
		return new ArtifactVersion(version, SNAPSHOT_SUFFIX);
	}

	/**
	 * Returns whether the version is a release version.
	 * 
	 * @return
	 */
	public boolean isReleaseVersion() {
		return suffix.equals(RELEASE_SUFFIX);
	}

	/**
	 * Returns whether the version is a milestone version.
	 * 
	 * @return
	 */
	public boolean isMilestoneVersion() {
		return suffix.matches(MILESTONE_SUFFIX);
	}

	public ArtifactVersion getNextDevelopmentVersion() {

		if (suffix.equals(SNAPSHOT_SUFFIX)) {
			return this;
		}

		if (suffix.equals(RELEASE_SUFFIX)) {
			return new ArtifactVersion(version.nextBugfix(), SNAPSHOT_SUFFIX);
		}

		return new ArtifactVersion(version, SNAPSHOT_SUFFIX);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ArtifactVersion that) {
		return this.toString().compareTo(that.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s.%s", version.toMajorMinorBugfix(), suffix);
	}

	/**
	 * Returns the {@link String} of the plain version (read: x.y.z, ommitting trailing bugfix zeros).
	 * 
	 * @return
	 */
	public String toShortString() {
		return version.toString();
	}
}
