/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.release.issues.github;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit tests for {@link GitHubWriteIssue}.
 *
 * @author Mark Paluch
 */
@JsonTest
@ExtendWith(SpringExtension.class)
class GitHubIssueUnitTests {

	@Autowired private JacksonTester<Milestone> json;

	@Test
	void shouldNotRenderOpenProperty() throws IOException {

		Milestone milestone = Milestone.of("my-title", "my-description");

		assertThat(this.json.write(milestone)) //
				.hasJsonPathStringValue("@.title", "my-title") //
				.hasJsonPathStringValue("@.description", "my-description") //
				.doesNotHaveJsonPathValue("@.open");
	}
}
