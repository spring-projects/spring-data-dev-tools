/*
 * Copyright 2022 the original author or authors.
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
 * Unit tests for {@link JavaVersion}.
 *
 * @author Mark Paluch
 */
class JavaVersionUnitTests {

	@Test
	void shouldParseDockerTag() {

		assertThat(JavaVersion.fromDockerTag("8u312-b07-jdk").getName()).isEqualTo("Java 1.8.0_312");
		assertThat(JavaVersion.fromDockerTag("8u322-b06-jre-focal").getName()).isEqualTo("Java 1.8.0_322");
		assertThat(JavaVersion.fromDockerTag("11.0.13_8-jdk").getName()).isEqualTo("Java 11.0.13 (Temurin)");
		assertThat(JavaVersion.fromDockerTag("17.0.1_12").getName()).isEqualTo("Java 17.0.1 (Temurin)");
	}

	@Test
	void shouldConsiderImplementor() {

		assertThat(JavaVersion.fromDockerTag("8u312-b07-jdk").withImplementor("Temurin").getName())
				.isEqualTo("Java 1.8.0_312 (Temurin)");
	}
}
