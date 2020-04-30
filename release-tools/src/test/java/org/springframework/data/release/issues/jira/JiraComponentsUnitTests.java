/*
 * Copyright 2016-2020 the original author or authors.
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
package org.springframework.data.release.issues.jira;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JiraComponents}.
 *
 * @author Mark Paluch
 */
class JiraComponentsUnitTests {

	@Test // #5
	void returnsComponentByName() {

		JiraComponent fooComponent = new JiraComponent("123", "foo");
		JiraComponents jiraComponents = JiraComponents.of(Collections.singleton(fooComponent));

		assertThat(jiraComponents.findComponent("foo").isPresent()).isTrue();
	}

	@Test // #5
	void returnsEmptyIfComponentMissing() {

		JiraComponent fooComponent = new JiraComponent("123", "foo");
		JiraComponents jiraComponents = JiraComponents.of(Collections.singleton(fooComponent));

		assertThat(jiraComponents.findComponent("baz").isPresent()).isFalse();
	}

	@Test // #5
	void failsOnNullArgumentConstruction() {
		assertThatIllegalArgumentException().isThrownBy(() -> JiraComponents.of(null));
	}
}
