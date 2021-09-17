/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.release.dependency;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.data.util.Streamable;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.table.Table;

/**
 * Shell commands for dependency management.
 *
 * @author Mark Paluch
 */
@CliComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfrastructureCommands extends TimedCommand {

	public static final String MAVEN_PROPERTIES = "dependency-upgrade-maven.properties";

	DependencyOperations operations;
	ExecutorService executor;
	GitOperations git;
	Logger logger;

	@CliCommand(value = "infra maven check")
	public void check(@CliOption(key = "", mandatory = true) TrainIteration iteration,
			@CliOption(key = "all", mandatory = false) Boolean reportAll) throws IOException {

		git.checkout(iteration.getTrain());

		DependencyUpgradeProposals proposals = operations.getMavenWrapperDependencyUpgradeProposals(iteration);

		Files.write(Paths.get(MAVEN_PROPERTIES), proposals.asProperties(iteration).getBytes());

		Table summary = proposals.toTable(reportAll == null ? false : reportAll);

		logger.log(Projects.BUILD, "Upgrade summary:" + System.lineSeparator() + System.lineSeparator() + summary);
		logger.log(iteration, "Upgrade proposals written to " + MAVEN_PROPERTIES);
	}

	@CliCommand(value = "infra maven upgrade")
	public void upgrade(@CliOption(key = "", mandatory = true) TrainIteration iteration)
			throws IOException, InterruptedException {

		logger.log(iteration, "Applying Maven wrapper upgrades to Spring Data…");

		DependencyVersions dependencyVersions = loadDependencyUpgrades(iteration);

		if (dependencyVersions.isEmpty()) {
			throw new IllegalStateException("No version to upgrade found!");
		}

		git.checkout(iteration.getTrain(), false);

		List<Project> projectsToUpgrade = operations
				.getProjectsToUpgradeMavenWrapper(dependencyVersions.get(Dependencies.MAVEN), iteration);

		ExecutionUtils.run(executor, Streamable.of(projectsToUpgrade), project -> {

			ModuleIteration module = iteration.getModule(project);
			Tickets tickets = operations.getOrCreateUpgradeTickets(module, dependencyVersions);
			operations.upgradeMavenWrapperVersion(tickets, module, dependencyVersions);
			git.push(module);

			// Allow GitHub to catch up with ticket notifications.
			Thread.sleep(1500);

			operations.closeUpgradeTickets(module, tickets);
		});
	}

	private DependencyVersions loadDependencyUpgrades(TrainIteration iteration) throws IOException {

		if (!Files.exists(Paths.get(MAVEN_PROPERTIES))) {
			logger.log(iteration, "Cannot upgrade dependencies: " + MAVEN_PROPERTIES + " does not exist.");
		}

		Properties properties = new Properties();
		try (FileInputStream fis = new FileInputStream(MAVEN_PROPERTIES)) {
			properties.load(fis);
		}

		return DependencyUpgradeProposals.fromProperties(iteration, properties);
	}

}
