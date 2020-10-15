/*
 * Copyright 2017-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Unit tests for {@link MaintainedVersions}.
 *
 * @author Oliver Gierke
 */
class MaintainedVersionsUnitTests {

	@Test
	void considersMostRecentReleaseVersionTheMainOne() {

		MaintainedVersion ingalls = MaintainedVersion.of(Projects.COMMONS, ArtifactVersion.of("1.13.0.RELEASE"),
				ReleaseTrains.INGALLS, null, null);
		MaintainedVersion hopper = MaintainedVersion.of(Projects.COMMONS, ArtifactVersion.of("1.12.0.RELEASE"),
				ReleaseTrains.HOPPER, null, null);

		MaintainedVersions versions = MaintainedVersions.of(ingalls, hopper);

		assertThat(versions.isMainVersion(ingalls)).isTrue();
		assertThat(versions.isMainVersion(hopper)).isFalse();
	}
}
