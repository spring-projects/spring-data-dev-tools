/*
 * Copyright 2016 the original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MavenProperties}.
 * 
 * @author Oliver Gierke
 */
public class MavenPropertiesUnitTests {

	MavenProperties properties;

	@Before
	public void setUp() {

		this.properties = new MavenProperties();
		this.properties.setPlugins(Collections.singletonMap("versions", "org.codehaus.mojo:versions-maven-plugin:2.2"));
	}

	/**
	 * @see #8
	 */
	@Test
	public void expandsGoalsCorrectly() {

		assertThat(properties.getFullyQualifiedPlugin("versions:set"),
				is("org.codehaus.mojo:versions-maven-plugin:2.2:set"));
	}

	/**
	 * @see #8
	 */
	@Test
	public void doesNotExpandGoalStartingWithDash() {
		assertThat(properties.getFullyQualifiedPlugin("-versions:set"), is("-versions:set"));
	}

	/**
	 * @see #8
	 */
	@Test
	public void doesNotExpandGoalWithoutColon() {
		assertThat(properties.getFullyQualifiedPlugin("versions-set"), is("versions-set"));
	}
}
