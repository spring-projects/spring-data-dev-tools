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

package org.springframework.data.release.issues.jira;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.data.release.issues.jira.JiraComponent;
import org.springframework.data.release.issues.jira.JiraComponents;

/**
 * Unit tests for {@link JiraComponents}.
 * 
 * @author Mark Paluch
 */
public class JiraComponentsUnitTests {

	/**
	 * @see #5
	 */
	@Test
	public void returnsComponentByName() throws Exception {

		JiraComponent fooComponent = new JiraComponent("123", "foo");
		JiraComponents jiraComponents = JiraComponents.of(Collections.singleton(fooComponent));

		assertThat(jiraComponents.findComponent("foo").isPresent(), is(true));
	}

	/**
	 * @see #5
	 */
	@Test
	public void returnsEmptyIfComponentMissing() throws Exception {

		JiraComponent fooComponent = new JiraComponent("123", "foo");
		JiraComponents jiraComponents = JiraComponents.of(Collections.singleton(fooComponent));

		assertThat(jiraComponents.findComponent("baz").isPresent(), is(false));
	}

	/**
	 * @see #5
	 */
	@Test(expected = IllegalArgumentException.class)
	public void failsOnNullArgumentConstruction() throws Exception {

		JiraComponents.of(null);

		fail("Missing IllegalArgumentException");
	}
}
