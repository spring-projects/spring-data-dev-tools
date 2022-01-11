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
package org.springframework.data.release.build;

import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.model.JavaVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ProjectAware;
import org.springframework.plugin.core.Plugin;

/**
 * Plugin interface to back different build systems.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
interface BuildSystem extends Plugin<Project> {

	/**
	 * Updates the project descriptors for the given {@link ModuleIteration} using the given {@link UpdateInformation}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param updateInformation must not be {@literal null}.
	 */
	<M extends ProjectAware> M updateProjectDescriptors(M iteration, UpdateInformation updateInformation);

	/**
	 * Prepares the project descriptor of the {@link ModuleIteration} for the given release {@link Phase}.
	 *
	 * @param module must not be {@literal null}.
	 * @param phase must not be {@literal null}.
	 * @return
	 */
	ModuleIteration prepareVersion(ModuleIteration module, Phase phase);

	/**
	 * Deploy artifacts for the given {@link ModuleIteration} and return the {@link DeploymentInformation}.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	DeploymentInformation deploy(ModuleIteration module);

	/**
	 * Runs the distribution build.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	<M extends ProjectAware> M triggerDistributionBuild(M module);

	<M extends ProjectAware> M triggerBuild(M module);

	/**
	 * Triggers the pre-release checks for the given {@link ModuleIteration}.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	<M extends ProjectAware> M triggerPreReleaseCheck(M module);

	/**
	 * Verify general functionality and correctness of the build setup.
	 */
	void verify();

	/**
	 * Prepare the build system with a Java version.
	 *
	 * @param javaVersion
	 * @return
	 */
	BuildSystem withJavaVersion(JavaVersion javaVersion);
}
