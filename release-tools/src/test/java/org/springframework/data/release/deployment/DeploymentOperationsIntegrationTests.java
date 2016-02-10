/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.release.deployment;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.build.BuildOperations;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;

/**
 * @author Oliver Gierke
 */
public class DeploymentOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired GitOperations git;
	@Autowired BuildOperations build;
	@Autowired DeploymentOperations deployment;
	@Autowired ArtifactoryClient client;

	@Test
	public void testname() {

		Train train = ReleaseTrains.HOPPER;
		ModuleIteration buildModule = train.getModuleIteration(Iteration.M1, "build");

		git.update(train);
		build.prepareVersion(buildModule, Phase.PREPARE);

		DeploymentInformation information = build.buildAndDeployRelease(buildModule);

		try {
			deployment.promote(information);
		} finally {
			client.deleteArtifacts(information);
		}
	}
}
