/*
 * Copyright 2016-2017 the original author or authors.
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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.util.Optional;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@CliComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class BuildCommands extends TimedCommand {

	@NonNull BuildOperations build;
	@NonNull Workspace workspace;
	@NonNull GitOperations git;
	@NonNull Logger logger;

	/**
	 * Removes all Spring Data artifacts from the local repository.
	 *
	 * @throws IOException
	 */
	@CliCommand("build purge artifacts")
	public void purge() throws IOException {

		logger.log("Workspace", "Cleaning up workspace directory at %s.",
				workspace.getWorkingDirectory().getAbsolutePath());

		workspace.purge(build.getLocalRepository(),
				path -> build.getLocalRepository().relativize(path).startsWith("org/springframework/data"));
	}

	/**
	 * Triggers a build for all modules of the given {@link TrainIteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param projectKey can be {@literal null} or empty.
	 */
	@CliCommand("build")
	public void build(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "module") String projectKey) {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Optional<Project> project = Projects.byName(projectKey);

		project.ifPresent(it -> build.triggerBuild(iteration.getModule(it)));

		if (!project.isPresent()) {
			git.prepare(iteration);
			iteration.forEach(build::triggerBuild);
		}
	}

	/**
	 * @param iteration must not be {@literal null}.
	 */
	@CliCommand("build distribute")
	public void buildDistribute(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		Assert.notNull(iteration, "Train iteration must not be null!");

		git.prepare(iteration);
		build.distributeResources(iteration);
	}
}
