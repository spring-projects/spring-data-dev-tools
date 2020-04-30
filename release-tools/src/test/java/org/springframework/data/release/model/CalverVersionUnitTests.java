/*
 * Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * Unit tests for {@link Calver}.
 * 
 * @author Mark Paluch
 */
public class CalverVersionUnitTests {

	@Test
	public void shouldParseRelease() {

		Calver version = Calver.parse("2020.0.1");

		assertThat(version.getYear()).isEqualTo(2020);
		assertThat(version.getMinor()).isEqualTo(0);
		assertThat(version.getMicro()).isEqualTo(1);
	}

	@Test
	public void shouldParseM1Release() {

		Calver version = Calver.parse("2020.0.1-M1");

		assertThat(version.getYear()).isEqualTo(2020);
		assertThat(version.getMinor()).isEqualTo(0);
		assertThat(version.getMicro()).isEqualTo(1);
		assertThat(version.getModifier()).isEqualTo(Iteration.M1);
	}

	@Test
	public void shouldCompareReleasesCorrectly() {

		Calver version = Calver.parse("2020.0.1-RC2");

		assertThat(version).isGreaterThan(Calver.parse("2020.0.0-RC2"));
		assertThat(version).isLessThan(Calver.parse("2020.0.2-RC2"));
		assertThat(version).isLessThan(Calver.parse("2020.1.0-RC2"));
		assertThat(version).isLessThan(Calver.parse("2021.0.0-RC2"));

		assertThat(version).isGreaterThan(Calver.parse("2020.0.1-RC1"));
		assertThat(version).isLessThan(Calver.parse("2020.0.1"));

		assertThat(Calver.parse("2020.0.1")).isEqualTo(Calver.parse("2020.0.1"));
		assertThat(Calver.parse("2020.0.1")).isLessThan(Calver.parse("2020.0.1-SR1"));
		assertThat(Calver.parse("2020.0.1")).isGreaterThan(Calver.parse("2020.0.1-M1"));
	}

	@Test
	public void shouldParseSnapshot() {

		Calver version = Calver.parse("2020.0.1-SNAPSHOT");

		assertThat(version.getYear()).isEqualTo(2020);
		assertThat(version.getMinor()).isEqualTo(0);
		assertThat(version.getMicro()).isEqualTo(1);
		assertThat(version.getModifier()).isEqualTo(new Iteration("SNAPSHOT", null));
	}
}
