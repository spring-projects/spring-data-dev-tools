/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.release.git;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.data.release.model.Projects.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.TestReleaseTrains;

/**
 * @author Oliver Gierke
 */
@Disabled
class GitOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired GitOperations gitOperations;

	@BeforeAll
	static void beforeClass() {

		try {
			URL url = new URL("https://github.com");
			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
			urlConnection.getInputStream().close();
		} catch (IOException e) {
			assumeTrue("Test requires connectivity to GitHub:" + e.toString(), false);
		}
	}

	@Test
	void updatesGitRepositories() throws Exception {
		gitOperations.update(ReleaseTrains.GOSLING);
	}

	@Test
	void showTags() throws Exception {

		gitOperations.update(TestReleaseTrains.SAMPLE);

		assertThat(gitOperations.getTags(BUILD).asList()).isNotEmpty();
	}

	@Test
	void foo() throws Exception {
		gitOperations.update(TestReleaseTrains.SAMPLE);
	}

	@Test
	void obtainsVersionTagsForRepoThatAlsoHasOtherTags() {
		gitOperations.getTags(MONGO_DB);
	}
}
