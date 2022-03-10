/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.release.infra;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;

import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.git.Branch;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

/**
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfrastructureOperations extends TimedCommand {

	public static final String CI_PROPERTIES = "ci/pipeline.properties";

	public static final String MAVEN_PROPERTIES = "dependency-upgrade-maven.properties";

	DependencyOperations dependencies;
	Workspace workspace;
	GitOperations git;
	ExecutorService executor;
	Logger logger;

	/**
	 * Distribute {@link #CI_PROPERTIES} from {@link Projects#BUILD} to all modules within {@link TrainIteration}.
	 *
	 * @param iteration
	 */
	void distributeCiProperties(TrainIteration iteration) {

		File master = workspace.getFile(CI_PROPERTIES, Projects.BUILD);

		if (!master.exists()) {
			throw new IllegalStateException(String.format("CI Properties file %s does not exist", master));
		}

		ExecutionUtils.run(executor, iteration, module -> {

			Project project = module.getProject();
			Branch branch = Branch.from(module);

			git.update(project);
			git.checkout(project, branch);
		});

		verifyExistingPropertyFiles(iteration.getTrain(), master);

		ExecutionUtils.run(executor, Streamable.of(iteration.getModulesExcept(Projects.BUILD)), module -> {

			File target = workspace.getFile(CI_PROPERTIES, module.getProject());
			target.delete();

			FileUtils.copyFile(master, target);

			git.add(module.getProject(), CI_PROPERTIES);
			git.commit(module, "Update CI properties.", Optional.empty(), false);
			git.push(module);
		});
	}

	private void verifyExistingPropertyFiles(Train train, File master) {

		for (Module module : train) {
			File target = workspace.getFile(CI_PROPERTIES, module.getProject());

			if (!target.exists()) {
				throw new IllegalStateException(String.format("CI Properties file %s does not exist", master));
			}
		}
	}

	public void upgradeMavenVersion(TrainIteration iteration) {

		DependencyVersions dependencyVersions = loadDependencyUpgrades(iteration);

		if (dependencyVersions.isEmpty()) {
			throw new IllegalStateException("No version to upgrade found!");
		}

		git.checkout(iteration.getTrain(), false);

		List<Project> projectsToUpgrade = dependencies
				.getProjectsToUpgradeMavenWrapper(dependencyVersions.get(Dependencies.MAVEN), iteration);

		ExecutionUtils.run(executor, Streamable.of(projectsToUpgrade), project -> {

			ModuleIteration module = iteration.getModule(project);
			Tickets tickets = dependencies.getOrCreateUpgradeTickets(module, dependencyVersions);
			dependencies.upgradeMavenWrapperVersion(tickets, module, dependencyVersions);
			git.push(module);

			// Allow GitHub to catch up with ticket notifications.
			Thread.sleep(1500);

			dependencies.closeUpgradeTickets(module, tickets);
		});
	}

	@SneakyThrows
	private DependencyVersions loadDependencyUpgrades(TrainIteration iteration) {

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
