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

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ArtifactVersion}.
 * 
 * @author Oliver Gierke
 */
class ArtifactVersionUnitTests {

	@Test
	void rejectsInvalidVersionSuffix() {
		Assertions.assertThatIllegalArgumentException().isThrownBy(() -> ArtifactVersion.of("1.4.5.GA"));
	}

	@Test
	void parsesReleaseVersionCorrectly() {

		assertThat(ArtifactVersion.of("1.4.5.RELEASE").isReleaseVersion()).isTrue();
		assertThat(ArtifactVersion.of("1.4.5.RELEASE").getNextDevelopmentVersion()).isEqualTo(ArtifactVersion.of("1.4.6.BUILD-SNAPSHOT"));

		assertThat(ArtifactVersion.of("1.4.5").isReleaseVersion()).isTrue();
		assertThat(ArtifactVersion.of("1.4.5").getNextDevelopmentVersion()).isEqualTo(ArtifactVersion.of("1.4.6-SNAPSHOT"));
	}

	@Test
	void createsMilestoneVersionCorrectly() {

		assertThat(ArtifactVersion.of("1.4.5.M1").isReleaseVersion()).isFalse();
		assertThat(ArtifactVersion.of("1.4.5.M1").isMilestoneVersion()).isTrue();

		assertThat(ArtifactVersion.of("1.4.5-M1").isReleaseVersion()).isFalse();
		assertThat(ArtifactVersion.of("1.4.5-M1").isMilestoneVersion()).isTrue();
	}

	@Test
	void createsReleaseVersionByDefault() {

		ArtifactVersion version = ArtifactVersion.of(Version.of(1, 4, 5));

		assertThat(version.isReleaseVersion()).isTrue();
		assertThat(version).hasToString("1.4.5.RELEASE");
	}

	@Test
	void createsMilestoneVersionFromIteration() {

		IterationVersion oneFourMilestoneOne = new SimpleIterationVersion(Version.of(1, 4), Iteration.M1);
		ArtifactVersion version = ArtifactVersion.of(oneFourMilestoneOne);

		assertThat(version.isMilestoneVersion()).isTrue();
		assertThat(version).hasToString("1.4.0.M1");
	}

	@Test
	void createsReleaseVersionFromIteration() {

		IterationVersion oneFourGA = new SimpleIterationVersion(Version.of(1, 4), Iteration.GA);
		ArtifactVersion version = ArtifactVersion.of(oneFourGA);

		assertThat(version.isReleaseVersion()).isTrue();
		assertThat(version).hasToString("1.4.0.RELEASE");
	}

	@Test
	void createsServiceReleaseVersionFromIteration() {

		IterationVersion oneFourServiceReleaseTwo = new SimpleIterationVersion(Version.of(1, 4), Iteration.SR2);
		ArtifactVersion version = ArtifactVersion.of(oneFourServiceReleaseTwo);

		assertThat(version.isReleaseVersion()).isTrue();
		assertThat(version).hasToString("1.4.2.RELEASE");
	}

	@Test
	void returnsNextMinorSnapshotVersionForGARelease() {

		ArtifactVersion version = ArtifactVersion.of("1.5.0.RELEASE").getNextDevelopmentVersion();

		assertThat(version.isMilestoneVersion()).isFalse();
		assertThat(version.isReleaseVersion()).isFalse();
		assertThat(version).isEqualTo(ArtifactVersion.of("1.6.0.BUILD-SNAPSHOT"));
	}

	@Test
	void ordersCorrectly() {

		ArtifactVersion oneNine = ArtifactVersion.of("1.9.0.RELEASE");
		ArtifactVersion oneTen = ArtifactVersion.of("1.10.0.RELEASE");

		assertThat(oneNine.compareTo(oneTen)).isLessThan(0);
	}

	@Test
	void ordersSnapshotsOfSameVersionSmaller() {

		ArtifactVersion oneTenRelease = ArtifactVersion.of("1.10.0.RELEASE");
		ArtifactVersion oneTenSnapshot = ArtifactVersion.of("1.10.0.BUILD-SNAPSHOT");

		assertThat(oneTenRelease.compareTo(oneTenSnapshot)).isGreaterThan(0);
	}

	@Test
	void returnsCorrectBugfixVersions() {

		assertThat(ArtifactVersion.of("1.0.0.RELEASE").getNextBugfixVersion())
				.isEqualTo(ArtifactVersion.of("1.0.1.BUILD-SNAPSHOT"));
		assertThat(ArtifactVersion.of("1.0.0.M1").getNextBugfixVersion())
				.isEqualTo(ArtifactVersion.of("1.0.0.BUILD-SNAPSHOT"));
		assertThat(ArtifactVersion.of("1.0.1.RELEASE").getNextBugfixVersion())
				.isEqualTo(ArtifactVersion.of("1.0.2.BUILD-SNAPSHOT"));
	}
}
