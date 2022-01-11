/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.data.release.cli;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * @author Oliver Gierke
 */
@Disabled("Skip GitHub interaction")
class ReleaseCommandsIntegrationTests extends AbstractIntegrationTests {

	@Autowired ReleaseCommands releaseCommands;
	@Autowired GitOperations git;

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
	void predictsReleaseTrainCorrectly() throws Exception {

		git.update(ReleaseTrains.MOORE);

		assertThat(releaseCommands.predictTrainAndIteration()).isEqualTo("Neumann");
	}
}
