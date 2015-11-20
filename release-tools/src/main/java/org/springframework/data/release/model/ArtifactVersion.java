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

import org.springframework.util.Assert;

/**
 * Value object to represent version of a particular artifact.
 * 
 * @author Oliver Gierke
 */
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
	 * @param suffix must not be {@literal null} or empty.
	 */
	private ArtifactVersion(Version version, String suffix) {

		Assert.notNull(version, "Version must not be null!");
		Assert.hasText(suffix, "Suffix must not be null or empty!");
		;

		this.version = version;
		this.suffix = suffix;
	}

	public static ArtifactVersion of(Version version) {
		return new ArtifactVersion(version, RELEASE_SUFFIX);
	}

	/**
	 * Parses the given {@link String} into an {@link ArtifactVersion}.
	 * 
	 * @param source must not be {@literal null} or empty.
	 * @return
	 */
	public static ArtifactVersion of(String source) {

		Assert.hasText(source, "Version source must not be null or empty!");

		int suffixStart = source.lastIndexOf('.');

		Version version = Version.parse(source.substring(0, suffixStart));
		String suffix = source.substring(suffixStart + 1);

		Assert.isTrue(suffix.matches(VALID_SUFFIX), String.format("Invalid version suffix: %s!", source));

		return new ArtifactVersion(version, suffix);
	}

	/**
	 * Creates a new {@link ArtifactVersion} from the given {@link IterationVersion}.
	 * 
	 * @param iterationVersion must not be {@literal null}.
	 * @return
	 */
	public static ArtifactVersion of(IterationVersion iterationVersion) {

		Assert.notNull(iterationVersion, "IterationVersion must not be null!");

		Version version = iterationVersion.getVersion();
		Iteration iteration = iterationVersion.getIteration();

		if (iteration.isGAIteration()) {
			return new ArtifactVersion(version, RELEASE_SUFFIX);
		}

		if (iteration.isServiceIteration()) {
			Version bugfixVersion = version.withBugfix(iteration.getBugfixValue());
			return new ArtifactVersion(bugfixVersion, RELEASE_SUFFIX);
		}

		return new ArtifactVersion(version, iteration.getName());
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

	/**
	 * Returns the next development version to be used for the current release version, which means next minor for GA
	 * versions and next bug fix for service releases. Will return the current version as snapshot otherwise.
	 * 
	 * @return
	 */
	public ArtifactVersion getNextDevelopmentVersion() {

		if (suffix.equals(RELEASE_SUFFIX)) {

			boolean isGaVersion = version.withBugfix(0).equals(version);
			Version nextVersion = isGaVersion ? version.nextMinor() : version.nextBugfix();

			return new ArtifactVersion(nextVersion, SNAPSHOT_SUFFIX);
		}

		return suffix.equals(SNAPSHOT_SUFFIX) ? this : new ArtifactVersion(version, SNAPSHOT_SUFFIX);
	}

	/**
	 * Returns the next bug fix version for the current version if it's a release version or the snapshot version of the
	 * current one otherwise.
	 * 
	 * @return
	 */
	public ArtifactVersion getNextBugfixVersion() {

		if (suffix.equals(RELEASE_SUFFIX)) {
			return new ArtifactVersion(version.nextBugfix(), SNAPSHOT_SUFFIX);
		}

		return suffix.equals(SNAPSHOT_SUFFIX) ? this : new ArtifactVersion(version, SNAPSHOT_SUFFIX);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ArtifactVersion that) {

		int versionsEqual = this.version.compareTo(that.version);
		return versionsEqual != 0 ? versionsEqual : this.suffix.compareTo(that.suffix);
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
	 * Returns the {@link String} of the plain version (read: x.y.z, omitting trailing bug fix zeros).
	 * 
	 * @return
	 */
	public String toShortString() {
		return version.toString();
	}
}
