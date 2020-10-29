/*
 * Copyright 2015-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for release {@link Train}s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class TrainsUnitTest {

	@Test
	void prefersNewVersionOfAdditionalModule() {

		Module module = ReleaseTrains.HOPPER.getModule(Projects.NEO4J);

		assertThat(module.getVersion()).isEqualTo(Version.parse("4.1"));
	}

	@Test
	void addsNewlyAddedModule() {
		assertThat(ReleaseTrains.HOPPER.getModule(Projects.ENVERS)).isNotNull();
	}

	@Test
	void considersCalverInBom() {

		assertThat(ReleaseTrains.OCKHAM.getModule(Projects.BOM).getVersion().toMajorMinorBugfix()).isEqualTo("2020.0.0");

		assertThat(ReleaseTrains.PASCAL.getModule(Projects.BOM).getVersion().toMajorMinorBugfix()).isEqualTo("2021.0.0");
	}
}
