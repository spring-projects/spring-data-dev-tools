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

import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.UpdateInformation;
import org.springframework.plugin.core.Plugin;

/**
 * Plugin interface to back different build systems.
 * 
 * @author Oliver Gierke
 */
interface BuildSystem extends Plugin<Project> {

	/**
	 * @param iteration
	 * @param train
	 * @param phase
	 * @throws Exception
	 */
	ModuleIteration updateProjectDescriptors(ModuleIteration iteration, UpdateInformation updateInformation);

	ModuleIteration prepareVersion(ModuleIteration module, Phase phase);

	ModuleIteration triggerDistributionBuild(ModuleIteration module);

	DeploymentInformation deploy(ModuleIteration module);
}
