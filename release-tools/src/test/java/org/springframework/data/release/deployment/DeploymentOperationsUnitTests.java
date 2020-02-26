/*
 * Copyright 2019-2020 the original author or authors.
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

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.utils.Logger;

/**
 * Unit tests for {@link DeploymentOperations}.
 *
 * @author Oliver Drotbohm
 */
public class DeploymentOperationsUnitTests {

	@Test // #113
	public void skipsPromotionForPublicArtifacts() {

		Logger logger = mock(Logger.class);
		ArtifactoryClient client = mock(ArtifactoryClient.class);
		DeploymentOperations operations = new DeploymentOperations(client, logger);

		ModuleIteration module = ReleaseTrains.MOORE.getModuleIteration(Projects.COMMONS, Iteration.GA);

		DefaultDeploymentInformation information = new DefaultDeploymentInformation(module, new DeploymentProperties());

		operations.promote(information);

		verify(logger).log(eq(module), anyString());
		verifyNoInteractions(client);
	}
}
