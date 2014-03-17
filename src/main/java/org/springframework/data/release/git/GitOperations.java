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
package org.springframework.data.release.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.io.CommandExecution;
import org.springframework.data.release.io.OsCommandOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GitOperations {

	private static final Logger LOGGER = HandlerUtils.getLogger(GitOperations.class);

	private final GitServer server = new GitServer();
	private final OsCommandOperations osCommandOperations;
	private final Workspace workspace;

	public GitProject getGitProject(Project project) {
		return new GitProject(project, server);
	}

	public void update(Train train) throws IOException, InterruptedException {

		List<CommandExecution> executions = new ArrayList<>();

		for (Module module : train) {
			executions.add(update(module.getProject()));
		}

		for (CommandExecution execution : executions) {
			execution.waitForResult();
		}
	}

	public CommandExecution update(Project project) throws IOException {

		GitProject gitProject = new GitProject(project, server);
		String repositoryName = gitProject.getRepositoryName();

		if (workspace.hasProjectDirectory(project)) {

			LOGGER.info(String.format("Found existing repository %s. Obtaining latest changes…", repositoryName));
			return osCommandOperations.executeCommand("git pull origin master", project);

		} else {

			File projectDirectory = workspace.getProjectDirectory(project);

			LOGGER.info(String.format("No repository found for project %s. Cloning repository from %s…", repositoryName,
					gitProject.getProjectUri()));
			return osCommandOperations.executeCommand(String.format("git clone %s %s", gitProject.getProjectUri(),
					projectDirectory.getName()));
		}
	}

	public List<Tag> getTags(Project project) throws IOException {

		CommandExecution command = osCommandOperations.executeCommand("git tag -l", project);
		List<Tag> tags = new ArrayList<>();

		for (String line : command.waitAndGetOutput().split("\n")) {
			tags.add(new Tag(line));
		}

		return tags;
	}
}
