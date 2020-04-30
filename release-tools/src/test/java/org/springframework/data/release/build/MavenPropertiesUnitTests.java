/*
 * Copyright 2016-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MavenProperties}.
 * 
 * @author Oliver Gierke
 */
class MavenPropertiesUnitTests {

	MavenProperties properties;

	@BeforeEach
	void setUp() {

		this.properties = new MavenProperties();
		this.properties.setPlugins(Collections.singletonMap("versions", "org.codehaus.mojo:versions-maven-plugin:2.2"));
	}

	@Test // #8
	void expandsGoalsCorrectly() {

		assertThat(properties.getFullyQualifiedPlugin("versions:set"))
				.isEqualTo("org.codehaus.mojo:versions-maven-plugin:2.2:set");
	}

	@Test // #8
	void doesNotExpandGoalStartingWithDash() {
		assertThat(properties.getFullyQualifiedPlugin("-versions:set")).isEqualTo("-versions:set");
	}

	@Test // #8
	void doesNotExpandGoalWithoutColon() {
		assertThat(properties.getFullyQualifiedPlugin("versions-set")).isEqualTo("versions-set");
	}
}
