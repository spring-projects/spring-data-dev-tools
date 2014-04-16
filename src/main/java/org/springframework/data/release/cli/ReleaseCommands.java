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

import static org.springframework.data.release.model.Projects.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.git.Tags;
import org.springframework.data.release.maven.MavenOperations;
import org.springframework.data.release.maven.Pom;
import org.springframework.data.release.misc.ReleaseOperations;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseCommands implements CommandMarker {

	private final MavenOperations maven;
	private final GitOperations git;
	private final ReleaseOperations misc;

	@CliCommand("release predict")
	public String predictTrainAndIteration() throws Exception {

		Pom pom = maven.getMavenProject(COMMONS);

		Tags tags = git.getTags(COMMONS);

		ArtifactVersion version = tags.getLatest().toArtifactVersion();
		System.out.println(version);

		for (Train train : ReleaseTrains.TRAINS) {

			Module module = train.getModule(COMMONS);

			if (!pom.getVersion().toString().startsWith(module.getVersion().toMajorMinorBugfix())) {
				continue;
			}

			return train.getName();
		}

		return null;
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
		maven.triggerDistributionBuild(iteration);
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
		maven.updatePom(iteration, Phase.PREPARE);
	}

	@CliCommand(value = "release conclude")
	public void conclude(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {

		// - pull updates
		// - tag release
		// - post release pom updates

		maven.updatePom(iteration, Phase.CLEANUP);

		// - push
	}
}
