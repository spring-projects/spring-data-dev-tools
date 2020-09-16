/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.release.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Value object to represent version of a particular artifact.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@EqualsAndHashCode
public class ArtifactVersion implements Comparable<ArtifactVersion> {

	private static final Pattern PATTERN = Pattern
			.compile("(\\d+)\\.(\\d+)(\\.\\d+)?(\\.((SR\\d+)|(RC\\d+)|(M\\d+)|(BUILD-SNAPSHOT)|(RELEASE)))");

	private static final Pattern MODIFIER_PATTERN = Pattern
			.compile("((\\d+)\\.(\\d+)(\\.\\d+)?)(-((RC\\d+)|(M\\d+)|(SNAPSHOT)))?");

	private static final String RELEASE_SUFFIX = "RELEASE";
	private static final String MILESTONE_SUFFIX = "M\\d|RC\\d";
	private static final String SNAPSHOT_SUFFIX = "BUILD-SNAPSHOT";
	private static final String SNAPSHOT_MODIFIER = "SNAPSHOT";

	private static final String VALID_SUFFIX = String.format("%s|%s|%s|-%s|-%s|-%s", RELEASE_SUFFIX, MILESTONE_SUFFIX,
			SNAPSHOT_SUFFIX, RELEASE_SUFFIX, MILESTONE_SUFFIX, SNAPSHOT_MODIFIER);

	private final Version version;
	private final @With boolean modifierFormat;
	private final @Getter String suffix;

	/**
	 * Creates a new {@link ArtifactVersion} from the given logical {@link Version}.
	 *
	 * @param version must not be {@literal null}.
	 * @param modifierFormat
	 * @param suffix must not be {@literal null} or empty.
	 */
	private ArtifactVersion(Version version, boolean modifierFormat, String suffix) {

		Assert.notNull(version, "Version must not be null!");
		Assert.hasText(suffix, "Suffix must not be null or empty!");

		this.version = version;
		this.modifierFormat = modifierFormat;
		this.suffix = suffix;
	}

	public static ArtifactVersion of(Version version) {
		return new ArtifactVersion(version, false, RELEASE_SUFFIX);
	}

	/**
	 * Parses the given {@link String} into an {@link ArtifactVersion}.
	 *
	 * @param source must not be {@literal null} or empty.
	 * @return
	 */
	public static ArtifactVersion of(String source) {

		Assert.hasText(source, "Version source must not be null or empty!");

		Matcher matcher = PATTERN.matcher(source);
		if (matcher.matches()) {

			int suffixStart = source.lastIndexOf('.');

			Version version = Version.parse(source.substring(0, suffixStart));
			String suffix = source.substring(suffixStart + 1);

			Assert.isTrue(suffix.matches(VALID_SUFFIX), String.format("Invalid version suffix: %s!", source));

			return new ArtifactVersion(version, false, suffix);
		}

		matcher = MODIFIER_PATTERN.matcher(source);

		if (matcher.matches()) {

			Version version = Version.parse(matcher.group(1));
			String suffix = matcher.group(6);

			return new ArtifactVersion(version, true, suffix == null ? RELEASE_SUFFIX : suffix);
		}

		throw new IllegalArgumentException(
				String.format("Version %s does not match <version>.<modifier> nor <version>-<modifier> pattern", source));
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
		boolean modifierVersionFormat = iterationVersion.usesModifierVersionFormat();

		if (iteration.isGAIteration()) {
			return new ArtifactVersion(version, modifierVersionFormat, RELEASE_SUFFIX);
		}

		if (iteration.isServiceIteration()) {
			Version bugfixVersion = version.withBugfix(iteration.getBugfixValue());
			return new ArtifactVersion(bugfixVersion, modifierVersionFormat, RELEASE_SUFFIX);
		}

		return new ArtifactVersion(version, modifierVersionFormat, iteration.getName());
	}

	public boolean isVersionWithin(Version version) {
		return this.version.toMajorMinorBugfix().startsWith(version.toString());
	}

	/**
	 * Returns the release version for the current one.
	 *
	 * @return
	 */
	public ArtifactVersion getReleaseVersion() {
		return new ArtifactVersion(version, modifierFormat, RELEASE_SUFFIX);
	}

	/**
	 * Returns the snapshot version of the current one.
	 *
	 * @return
	 */
	public ArtifactVersion getSnapshotVersion() {
		return new ArtifactVersion(version, modifierFormat, getSnapshotSuffix());
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

	public boolean isSnapshotVersion() {
		return suffix.matches(SNAPSHOT_SUFFIX) || suffix.matches(SNAPSHOT_MODIFIER);
	}

	public boolean isBugFixVersion() {
		return isReleaseVersion() && !version.toMajorMinorBugfix().endsWith(".0");
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

			return new ArtifactVersion(nextVersion, modifierFormat, getSnapshotSuffix());
		}

		return isSnapshotVersion() ? this : new ArtifactVersion(version, modifierFormat, getSnapshotSuffix());
	}

	/**
	 * Returns the next bug fix version for the current version if it's a release version or the snapshot version of the
	 * current one otherwise.
	 *
	 * @return
	 */
	public ArtifactVersion getNextBugfixVersion() {

		if (suffix.equals(RELEASE_SUFFIX)) {
			return new ArtifactVersion(version.nextBugfix(), modifierFormat, getSnapshotSuffix());
		}

		return isSnapshotVersion() ? this : new ArtifactVersion(version, modifierFormat, getSnapshotSuffix());
	}

	public String getReleaseTrainSuffix() {

		if (isSnapshotVersion() || isMilestoneVersion()) {
			return suffix;
		}

		if (isBugFixVersion()) {
			return "SR" + version.getBugfix();
		}

		return "GA";
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

		if (modifierFormat) {

			if (isSnapshotVersion() || isMilestoneVersion()) {
				return String.format("%s-%s", version.toMajorMinorBugfix(), suffix);
			}

			return version.toMajorMinorBugfix();
		}

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

	private String getSnapshotSuffix() {
		return modifierFormat ? SNAPSHOT_MODIFIER : SNAPSHOT_SUFFIX;
	}
}
