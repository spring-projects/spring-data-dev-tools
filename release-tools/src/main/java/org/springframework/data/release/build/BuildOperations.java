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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.assertj.core.util.VisibleForTesting;

import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor
public class BuildOperations {

	private final @NonNull PluginRegistry<BuildSystem, Project> buildSystems;
	private final @NonNull Logger logger;
	private final @NonNull MavenProperties properties;
	private final @NonNull BuildExecutor executor;

	/**
	 * Updates all inter-project dependencies based on the given {@link TrainIteration} and release {@link Phase}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param phase must not be {@literal null}.
	 * @throws Exception
	 */
	public void updateProjectDescriptors(TrainIteration iteration, Phase phase) throws Exception {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Assert.notNull(phase, "Phase must not be null!");

		UpdateInformation updateInformation = UpdateInformation.of(iteration, phase);

		BuildExecutor.Summary<ModuleIteration> summary = executor.doWithBuildSystemOrdered(iteration,
				(system, it) -> system.updateProjectDescriptors(it, updateInformation));

		logger.log(iteration, "Update Project Descriptors done: %s", summary);
	}

	/**
	 * Triggers the distribution builds for all modules participating in the given {@link TrainIteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 */
	public void distributeResources(TrainIteration iteration) {

		Assert.notNull(iteration, "Train iteration must not be null!");

		distributeResources(iteration.getTrain());
	}

	/**
	 * Triggers the distribution builds for all modules participating in the given {@link Train}.
	 *
	 * @param iteration must not be {@literal null}.
	 */
	public void distributeResources(Train train) {

		Assert.notNull(train, "Train must not be null!");

		BuildExecutor.Summary<Module> summary = executor.doWithBuildSystemAnyOrder(train,
				BuildSystem::triggerDistributionBuild);

		logger.log(train, "Distribution build: %s", summary);
	}

	/**
	 * Performs the release build for all modules in the given {@link TrainIteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @return
	 */
	public List<DeploymentInformation> performRelease(TrainIteration iteration) {

		BuildExecutor.Summary<DeploymentInformation> summary = executor.doWithBuildSystemOrdered(iteration,
				(buildSystem, moduleIteration) -> performRelease(moduleIteration));

		logger.log(iteration, "Release: %s", summary);

		return summary.getExecutions().stream().map(BuildExecutor.ExecutionResult::getResult).collect(Collectors.toList());
	}

	/**
	 * Performs the release build for the given {@link ModuleIteration}.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	public DeploymentInformation performRelease(ModuleIteration module) {
		return buildAndDeployRelease(module);
	}

	/**
	 * Prepares the versions of the given {@link TrainIteration} depending on the given {@link Phase}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param phase must not be {@literal null}.
	 */
	public void prepareVersions(TrainIteration iteration, Phase phase) {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Assert.notNull(phase, "Phase must not be null!");

		BuildExecutor.Summary<ModuleIteration> summary = executor.doWithBuildSystemOrdered(iteration,
				(system, module) -> system.prepareVersion(module, phase));

		logger.log(iteration, "Prepare versions: %s", summary);
	}

	/**
	 * Prepares the version of the given {@link ModuleIteration} depending on the given {@link Phase}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param phase must not be {@literal null}.
	 * @return
	 */
	@VisibleForTesting
	public ModuleIteration prepareVersion(ModuleIteration iteration, Phase phase) {

		Assert.notNull(iteration, "Module iteration must not be null!");
		Assert.notNull(phase, "Phase must not be null!");

		return doWithBuildSystem(iteration, (system, module) -> system.prepareVersion(module, phase));
	}

	/**
	 * Returns the {@link Path} of the local artifact repository.
	 *
	 * @return
	 */
	public Path getLocalRepository() {
		return properties.getLocalRepository().toPath();
	}

	/**
	 * Builds the release for the given {@link ModuleIteration} and deploys it to the staging repository.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	public DeploymentInformation buildAndDeployRelease(ModuleIteration module) {
		return doWithBuildSystem(module, BuildSystem::deploy);
	}

	/**
	 * Triggers a normal build for the given {@link ModuleIteration}.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	public ModuleIteration triggerBuild(ModuleIteration module) {
		return doWithBuildSystem(module, BuildSystem::triggerBuild);
	}

	/**
	 * Triggers the pre-release checks for all modules of the given {@link TrainIteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 */
	public void runPreReleaseChecks(TrainIteration iteration) {

		Assert.notNull(iteration, "Train iteration must not be null!");

		executor.doWithBuildSystemAnyOrder(iteration, BuildSystem::triggerPreReleaseCheck);
	}

	public void verify() {

		BuildSystem buildSystem = buildSystems.getRequiredPluginFor(Projects.BUILD);
		buildSystem.verify();
	}

	/**
	 * Selects the build system for the module contained in the given {@link ModuleIteration} and executes the given
	 * function with it.
	 *
	 * @param module must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	private <T> T doWithBuildSystem(ModuleIteration module, BiFunction<BuildSystem, ModuleIteration, T> function) {

		Assert.notNull(module, "ModuleIteration must not be null!");

		Supplier<IllegalStateException> exception = () -> new IllegalStateException(
				String.format("No build system plugin found for project %s!", module.getProject()));

		return function.apply(buildSystems.getPluginFor(module.getProject(), exception), module);
	}

}
