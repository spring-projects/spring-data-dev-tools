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
package org.springframework.data.release;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.release.build.MavenArtifact;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * @author Oliver Gierke
 */
public class ArtifactUnitTests {

	@Test
	public void testname() {

		MavenArtifact artifact = new MavenArtifact(ReleaseTrains.DIJKSTRA.getModuleIteration(Iteration.M1, "JPA"));

		assertThat(artifact.getArtifactId(), is("spring-data-jpa"));
		assertThat(artifact.getVersion(), is(ArtifactVersion.of("1.6.0.M1")));
		assertThat(artifact.getNextDevelopmentVersion(), is(ArtifactVersion.of("1.6.0.BUILD-SNAPSHOT")));
	}
}
