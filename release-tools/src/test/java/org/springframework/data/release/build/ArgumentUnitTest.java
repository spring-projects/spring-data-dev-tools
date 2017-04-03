/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.release.build;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.release.build.CommandLine.Argument;
import org.springframework.data.release.model.Password;

/**
 * Unit tests for {@link Argument}.
 * 
 * @author Oliver Gierke
 */
public class ArgumentUnitTest {

	@Test
	public void masksMaskedForToString() {

		Argument argument = Argument.of("password").withValue(Password.of("password"));

		assertThat(argument.toCommandLineArgument(), is("password=password"));
		assertThat(argument.toString(), startsWith("password=********"));
	}

	@Test
	public void quotesValue() {

		Argument argument = Argument.of("quoted").withQuotedValue("value");

		assertThat(argument.toCommandLineArgument(), is("quoted=\"value\""));
	}

	@Test
	public void argCreatesJvmArgument() {
		assertThat(Argument.arg("foo").toCommandLineArgument(), is("-Dfoo"));
	}

	@Test
	public void profileCreatesMavenProfile() {
		assertThat(Argument.profile("foo").toCommandLineArgument(), is("-Pfoo"));
		assertThat(Argument.profile("foo", "bar").toCommandLineArgument(), is("-Pfoo,bar"));
	}
}
