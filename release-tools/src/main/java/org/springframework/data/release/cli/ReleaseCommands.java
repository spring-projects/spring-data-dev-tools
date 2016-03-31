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

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.CliComponent;
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
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@CliComponent
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class ReleaseCommands implements CommandMarker {

	private final GitOperations git;
	private final ReleaseOperations misc;
	private final DeploymentOperations deployment;
	private final BuildOperations build;

	@CliCommand("release predict")
	public String predictTrainAndIteration() throws Exception {

		return git.getTags(COMMONS).getLatest().toArtifactVersion().//
				map(this::getTrainNameForCommonsVersion).//
				orElse(null);
	}

	public String getTrainNameForCommonsVersion(ArtifactVersion version) {

		return ReleaseTrains.TRAINS.stream().//
				filter(train -> version.toString().startsWith(train.getModule(COMMONS).getVersion().toString())).//
				findFirst().map(Train::getName).orElse(null);
	}

	/**
	 * Prepares the release of the given iteration of the given train.
	 * 
	 * @param trainName the name of the release train (ignoring case).
	 * @param iterationName the name of the iteration.
	 * @throws Exception
	 */
	@CliCommand(value = "release prepare", help = "Prepares the release of the iteration of the given train.")
	public void prepare(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {

		git.prepare(iteration);

		misc.prepareChangelogs(iteration);
		misc.updateResources(iteration);
		build.updateProjectDescriptors(iteration, Phase.PREPARE);
		git.commit(iteration, "Prepare %s.");

		build.prepareVersions(iteration, Phase.PREPARE);
		git.commit(iteration, "Release version %s.");
	}

	@CliCommand(value = "release build")
	public void buildRelease(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "project", mandatory = false) String projectName) throws Exception {

		if (projectName != null) {

			deployment.verifyAuthentication();

			Project project = Projects.byName(projectName);
			ModuleIteration module = iteration.getModule(project);

			DeploymentInformation information = build.performRelease(module);
			deployment.promote(information);

		} else {

			deployment.verifyAuthentication();

			List<DeploymentInformation> deploymentInformation = build.performRelease(iteration);

			deploymentInformation.forEach(deployment::promote);

			build.prepareVersions(iteration, Phase.CLEANUP);
			git.commit(iteration, "Prepare next development iteration.");
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

		// Tag release
		git.tagRelease(iteration);

		// Prepare master branch
		build.updateProjectDescriptors(iteration, Phase.CLEANUP);
		git.commit(iteration, "After release cleanups.");

		// Prepare maintenance branches
		if (iteration.getIteration().isGAIteration()) {

			git.createMaintenanceBranches(iteration);

			build.updateProjectDescriptors(iteration, Phase.MAINTENANCE);
			build.prepareVersions(iteration, Phase.MAINTENANCE);
			git.commit(iteration, "Prepare next development iteration.");

			git.checkout(iteration);
		}
	}

	/**
	 * Triggers the distribution of release artifacts for all projects.
	 * 
	 * @param trainName
	 * @param iterationName
	 * @throws Exception
	 */
	@CliCommand("release distribute")
	public void distribute(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {

		git.checkout(iteration);
		build.distributeResources(iteration);
	}
}
