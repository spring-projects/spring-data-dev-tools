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
package org.springframework.data.release.maven;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.IterationVersion;
import org.springframework.data.release.model.SimpleIterationVersion;
import org.springframework.data.release.model.Version;

/**
 * @author Oliver Gierke
 */
public class MavenVersionUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidVersionSuffix() {
		ArtifactVersion.parse("1.4.5.GA");
	}

	@Test
	public void parsesReleaseVersionCorrectly() {

		ArtifactVersion version = ArtifactVersion.parse("1.4.5.RELEASE");

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.getNextDevelopmentVersion(), is(ArtifactVersion.parse("1.4.6.BUILD-SNAPSHOT")));
	}

	@Test
	public void createsMilestoneVersionCorrectly() {

		ArtifactVersion version = ArtifactVersion.parse("1.4.5.M1");

		assertThat(version.isReleaseVersion(), is(false));
		assertThat(version.isMilestoneVersion(), is(true));
	}

	@Test
	public void createsReleaseVersionByDefault() {

		ArtifactVersion version = new ArtifactVersion(new Version(1, 4, 5));

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.toString(), is("1.4.5.RELEASE"));
	}

	@Test
	public void createsMilestoneVersionFromIteration() {

		IterationVersion oneFourMilestoneOne = new SimpleIterationVersion(new Version(1, 4), Iteration.M1);
		ArtifactVersion version = ArtifactVersion.from(oneFourMilestoneOne);

		assertThat(version.isMilestoneVersion(), is(true));
		assertThat(version.toString(), is("1.4.0.M1"));
	}

	@Test
	public void createsReleaseVersionFromIteration() {

		IterationVersion oneFourGA = new SimpleIterationVersion(new Version(1, 4), Iteration.GA);
		ArtifactVersion version = ArtifactVersion.from(oneFourGA);

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.toString(), is("1.4.0.RELEASE"));
	}

	@Test
	public void createsServiceReleaseVersionFromIteration() {

		IterationVersion oneFourServiceReleaseTwo = new SimpleIterationVersion(new Version(1, 4), Iteration.SR2);
		ArtifactVersion version = ArtifactVersion.from(oneFourServiceReleaseTwo);

		assertThat(version.isReleaseVersion(), is(true));
		assertThat(version.toString(), is("1.4.2.RELEASE"));
	}
}
