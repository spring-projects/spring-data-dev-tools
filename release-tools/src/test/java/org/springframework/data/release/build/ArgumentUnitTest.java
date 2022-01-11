/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.data.release.build;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.build.CommandLine.Argument;
import org.springframework.data.release.model.Password;

/**
 * Unit tests for {@link Argument}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class ArgumentUnitTest {

	@Test
	void masksMaskedForToString() {

		Argument argument = Argument.of("password").withValue(Password.of("password"));

		assertThat(argument.toCommandLineArgument()).isEqualTo("password=password");
		assertThat(argument.toString()).startsWith("password=********");
	}

	@Test
	void quotesValue() {

		Argument argument = Argument.of("quoted").withQuotedValue("value");

		assertThat(argument.toCommandLineArgument()).isEqualTo("quoted=\"value\"");
	}

	@Test
	void argCreatesJvmArgument() {
		assertThat(Argument.arg("foo").toCommandLineArgument()).isEqualTo("-Dfoo");
	}

	@Test
	void profileCreatesMavenProfile() {
		assertThat(Argument.profile("foo").toCommandLineArgument()).isEqualTo("-Pfoo");
		assertThat(Argument.profile("foo", "bar").toCommandLineArgument()).isEqualTo("-Pfoo,bar");
	}
}
