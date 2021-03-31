/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.git;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Unit tests for {@link VersionTags}.
 *
 * @author Mark Paluch
 */
class VersionTagsUnitTests {

	@Test
	void shouldDetermineTagForIteration() {

		VersionTags tags = new VersionTags(Projects.NEO4J,
				Stream.of("5.3.0.RELEASE", "5.4.0-M1", "6.0.0-M2").map(Tag::of).collect(Collectors.toList()));

		assertThat(tags.filter(ReleaseTrains.NEUMANN).findTag(Iteration.GA))
				.hasValueSatisfying(actual -> assertThat(actual.getName()).isEqualTo("5.3.0.RELEASE"));
		assertThat(tags.filter(ReleaseTrains.OCKHAM).findTag(Iteration.M2))
				.hasValueSatisfying(actual -> assertThat(actual.getName()).isEqualTo("6.0.0-M2"));
	}

	@Test
	void shouldDetermineTagForMajorVersionBumpDuringDevelopment() {

		VersionTags tags = new VersionTags(Projects.NEO4J,
				Stream.of("5.3.0.RELEASE", "5.4.0-M1", "6.0.0-M2").map(Tag::of).collect(Collectors.toList()));

		assertThat(tags.filter(ReleaseTrains.OCKHAM).findTag(Iteration.M1))
				.hasValueSatisfying(actual -> assertThat(actual.getName()).isEqualTo("5.4.0-M1"));
	}
}
