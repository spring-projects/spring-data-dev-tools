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
package org.springframework.data.release.dependency;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DependencyVersion}.
 *
 * @author Mark Paluch
 */
class DependencyVersionUnitTests {

	@Test
	void shouldConsiderSemVerSortOrder() {

		List<String> sorted = Stream.of("1.0.0", "1.0.0-m1", "1.0.0-rc1", "1.0.0-m2") //
				.map(DependencyVersion::of) //
				.sorted() //
				.map(DependencyVersion::getIdentifier) //
				.collect(Collectors.toList());

		assertThat(sorted).containsExactly("1.0.0-m1", "1.0.0-m2", "1.0.0-rc1", "1.0.0");
	}

	@Test
	void shouldConsiderReleaseTrainSortOrder() {

		List<String> sorted = Stream.of("Bismuth-SR1", "Aluminium-SR1", "Aluminium-RELEASE", "Aluminium-SR2") //
				.map(DependencyVersion::of) //
				.sorted() //
				.map(DependencyVersion::getIdentifier) //
				.collect(Collectors.toList());

		assertThat(sorted).containsExactly("Aluminium-RELEASE", "Aluminium-SR1", "Aluminium-SR2", "Bismuth-SR1");
	}
}
