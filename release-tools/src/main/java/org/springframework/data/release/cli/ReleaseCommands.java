/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.springframework.data.release.model.Projects.*;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.build.BuildOperations;
import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.deployment.DeploymentOperations;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.misc.ReleaseOperations;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@CliComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class ReleaseCommands extends TimedCommand {

	@NonNull GitOperations git;
	@NonNull ReleaseOperations misc;
	@NonNull DeploymentOperations deployment;
	@NonNull BuildOperations build;

	@CliCommand("release predict")
	public String predictTrainAndIteration() {

		return git.getTags(COMMONS).getLatest().toArtifactVersion().//
				map(ReleaseCommands::getTrainNameForCommonsVersion).//
				orElse(null);
	}

	/**
	 * Prepares the release of the given iteration of the given train.
	 *
	 * @param iteration
	 * @throws Exception
	 */
	@CliCommand(value = "release prepare", help = "Prepares the release of the iteration of the given train.")
	public void prepare(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {

		git.prepare(iteration);

		build.runPreReleaseChecks(iteration);

		misc.prepareChangelogs(iteration);
		misc.updateResources(iteration);
		build.updateProjectDescriptors(iteration, Phase.PREPARE);
		git.commit(iteration, "Prepare %s.");

		build.prepareVersions(iteration, Phase.PREPARE);
		git.commit(iteration, "Release version %s.");
	}

	@CliCommand(value = "release build")
	public void buildRelease(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "project", mandatory = false) String projectName) {

		if (!iteration.getIteration().isPublic()) {
			deployment.verifyAuthentication();
		}

		if (projectName != null) {

			Project project = Projects.requiredByName(projectName);
			ModuleIteration module = iteration.getModule(project);

			DeploymentInformation information = build.performRelease(module);
			deployment.promote(information);

		} else {

			build.performRelease(iteration).forEach(deployment::promote);
		}
	}

	/**
	 * Concludes the release of the given {@link TrainIteration}.
	 *
	 * @param iteration
	 * @throws Exception
	 */
	@CliCommand(value = "release conclude")
	public void conclude(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {

		Assert.notNull(iteration, "Train iteration must not be null!");

		build.prepareVersions(iteration, Phase.CLEANUP);
		git.commit(iteration, "Prepare next development iteration.");

		// Prepare master branch
		build.updateProjectDescriptors(iteration, Phase.CLEANUP);
		git.commit(iteration, "After release cleanups.");

		// Tag release
		git.tagRelease(iteration);

		// Prepare maintenance branches
		if (iteration.getIteration().isGAIteration()) {

			// Create bugfix branches
			git.createMaintenanceBranches(iteration);

			// Set project version to maintenance once
			build.prepareVersions(iteration, Phase.MAINTENANCE);
			git.commit(iteration, "Prepare next development iteration.");

			// Update inter-project dependencies and repositories
			build.updateProjectDescriptors(iteration, Phase.MAINTENANCE);
			git.commit(iteration, "After release cleanups.");

			// Back to master branch
			git.checkout(iteration);
		}
	}

	/**
	 * Triggers the distribution of release artifacts for all projects.
	 *
	 * @param iteration
	 * @throws Exception
	 */
	@CliCommand("release distribute")
	public void distribute(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		git.checkout(iteration);
		build.distributeResources(iteration);
	}

	private static String getTrainNameForCommonsVersion(ArtifactVersion version) {

		return ReleaseTrains.TRAINS.stream().//
				filter(train -> version.toString().startsWith(train.getModule(COMMONS).getVersion().toString())).//
				findFirst().map(Train::getName).orElse(null);
	}
}
