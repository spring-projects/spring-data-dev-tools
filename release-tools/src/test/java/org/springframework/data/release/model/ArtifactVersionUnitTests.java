/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Oliver Gierke
 */
public class ArtifactVersionUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidVersionSuffix() {
		ArtifactVersion.of("1.4.5.GA");
	}

	@Test
	public void parsesReleaseVersionCorrectly() {

		ArtifactVersion version = ArtifactVersion.of("1.4.5.RELEASE");

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.getNextDevelopmentVersion(), is(ArtifactVersion.of("1.4.6.BUILD-SNAPSHOT")));
	}

	@Test
	public void createsMilestoneVersionCorrectly() {

		ArtifactVersion version = ArtifactVersion.of("1.4.5.M1");

		assertThat(version.isReleaseVersion(), is(false));
		assertThat(version.isMilestoneVersion(), is(true));
	}

	@Test
	public void createsReleaseVersionByDefault() {

		ArtifactVersion version = ArtifactVersion.of(Version.of(1, 4, 5));

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.toString(), is("1.4.5.RELEASE"));
	}

	@Test
	public void createsMilestoneVersionFromIteration() {

		IterationVersion oneFourMilestoneOne = new SimpleIterationVersion(Version.of(1, 4), Iteration.M1);
		ArtifactVersion version = ArtifactVersion.of(oneFourMilestoneOne);

		assertThat(version.isMilestoneVersion(), is(true));
		assertThat(version.toString(), is("1.4.0.M1"));
	}

	@Test
	public void createsReleaseVersionFromIteration() {

		IterationVersion oneFourGA = new SimpleIterationVersion(Version.of(1, 4), Iteration.GA);
		ArtifactVersion version = ArtifactVersion.of(oneFourGA);

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.toString(), is("1.4.0.RELEASE"));
	}

	@Test
	public void createsServiceReleaseVersionFromIteration() {

		IterationVersion oneFourServiceReleaseTwo = new SimpleIterationVersion(Version.of(1, 4), Iteration.SR2);
		ArtifactVersion version = ArtifactVersion.of(oneFourServiceReleaseTwo);

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.toString(), is("1.4.2.RELEASE"));
	}

	@Test
	public void returnsNextMinorSnapshotVersionForGARelease() {

		ArtifactVersion version = ArtifactVersion.of("1.5.0.RELEASE").getNextDevelopmentVersion();

		assertThat(version.isMilestoneVersion(), is(false));
		assertThat(version.isReleaseVersion(), is(false));
		assertThat(version, is(ArtifactVersion.of("1.6.0.BUILD-SNAPSHOT")));
	}

	@Test
	public void ordersCorrectly() {

		ArtifactVersion oneNine = ArtifactVersion.of("1.9.0.RELEASE");
		ArtifactVersion oneTen = ArtifactVersion.of("1.10.0.RELEASE");

		assertThat(oneNine.compareTo(oneTen), is(lessThan(0)));
	}

	@Test
	public void ordersSnapshotsOfSameVersionSmaller() {

		ArtifactVersion oneTenRelease = ArtifactVersion.of("1.10.0.RELEASE");
		ArtifactVersion oneTenSnapshot = ArtifactVersion.of("1.10.0.BUILD-SNAPSHOT");

		assertThat(oneTenRelease.compareTo(oneTenSnapshot), is(greaterThan(0)));
	}

	@Test
	public void returnsCorrectBugfixVersions() {

		assertThat(ArtifactVersion.of("1.0.0.RELEASE").getNextBugfixVersion(),
				is(ArtifactVersion.of("1.0.1.BUILD-SNAPSHOT")));
		assertThat(ArtifactVersion.of("1.0.0.M1").getNextBugfixVersion(), is(ArtifactVersion.of("1.0.0.BUILD-SNAPSHOT")));
		assertThat(ArtifactVersion.of("1.0.1.RELEASE").getNextBugfixVersion(),
				is(ArtifactVersion.of("1.0.2.BUILD-SNAPSHOT")));
	}
}
