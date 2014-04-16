/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.release.cli;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * @author Oliver Gierke
 */
public class ReleaseCommandsIntegrationTests extends AbstractIntegrationTests {

	@Autowired ReleaseCommands releaseCommands;
	@Autowired GitOperations git;

	@Test
	@Ignore
	public void predictsReleasTrainCorrectly() throws Exception {

		git.update(ReleaseTrains.DIJKSTRA);

		assertThat(releaseCommands.predictTrainAndIteration(), is("Dijkstra"));
	}
}
