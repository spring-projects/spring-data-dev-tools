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
package org.springframework.data.release.build;

import static org.springframework.data.release.model.Projects.*;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.deployment.DeploymentProperties;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;

/**
 * Gradle specific operations.
 * 
 * @author Oliver Gierke
 */
@Component
@Order(200)
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
class GradleBuildSystem implements BuildSystem {

	private static final String BUILD_GRADLE = "build.gradle";
	private static final String GRADLE_PROPERTIES = "gradle.properties";
	private static final String COMMONS_PROPERTY = "springDataCommonsVersion";
	private static final String BUILD_PROPERTY = "springDataBuildVersion";

	private final Workspace workspace;
	private final Logger logger;
	private final DeploymentProperties properties;

	/**
	 * Updates all Gradle projects contained in the release.
	 * 
	 * @param iteration
	 * @param phase
	 * @throws Exception
	 */
	public void updateProject(TrainIteration iteration, final Phase phase) throws Exception {

		UpdateInformation updateInformation = UpdateInformation.of(iteration, phase);

		for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {
			updateProjectDescriptors(module, updateInformation);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#updateProjectDescriptors(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.model.UpdateInformation)
	 */
	@Override
	public ModuleIteration updateProjectDescriptors(ModuleIteration iteration, UpdateInformation updateInformation) {

		Project project = iteration.getProject();
		Repository repository = new Repository(iteration.getIteration());
		ArtifactVersion commonsVersion = updateInformation.getIteration().getModuleVersion(COMMONS);
		ArtifactVersion buildVersion = updateInformation.getIteration().getModuleVersion(BUILD);

		workspace.processFile(GRADLE_PROPERTIES, project, (line, number) -> {

			if (line.contains(COMMONS_PROPERTY)) {

				ArtifactVersion version = updateInformation.getPhase().equals(Phase.PREPARE) ? commonsVersion
						: commonsVersion.getNextDevelopmentVersion();

				logger.log(project, "Setting Spring Data Commons version in %s to %s.", GRADLE_PROPERTIES, version);
				return String.format("%s=%s", COMMONS_PROPERTY, version);
			}

			if (line.contains(BUILD_PROPERTY)) {

				ArtifactVersion version = updateInformation.getPhase().equals(Phase.PREPARE) ? buildVersion
						: buildVersion.getNextDevelopmentVersion();

				logger.log(project, "Setting Spring Data Build version in %s to %s.", GRADLE_PROPERTIES, version);
				return String.format("%s=%s", BUILD_PROPERTY, version);
			}

			return line;
		});

		workspace.processFile(BUILD_GRADLE, project, (line, number) -> {

			String snapshotUrl = repository.getSnapshotUrl();
			String releaseUrl = repository.getUrl();
			String message = "Switching to Spring repository %s";

			switch (updateInformation.getPhase()) {
				case CLEANUP:
					logger.log(project, message, snapshotUrl);
					return line.contains(releaseUrl) ? line.replace(releaseUrl, snapshotUrl) : line;
				case PREPARE:
				default:
					logger.log(project, message, releaseUrl);
					return line.contains(snapshotUrl) ? line.replace(snapshotUrl, releaseUrl) : line;
			}
		});

		return iteration;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#prepareVersion(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.model.Phase)
	 */
	@Override
	public ModuleIteration prepareVersion(ModuleIteration module, Phase phase) {
		return module;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#triggerDistributionBuild(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public ModuleIteration triggerDistributionBuild(ModuleIteration module) {
		return module;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#deploy(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public DeploymentInformation deploy(ModuleIteration module) {
		return new DeploymentInformation(module, properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return isGradleProject(project);
	}

	/**
	 * Returns whether the given project is a Gradle project (checks for the presence of a build.gradle file).
	 * 
	 * @param project
	 * @return
	 */
	private boolean isGradleProject(Project project) {
		return workspace.getFile(BUILD_GRADLE, project).exists();
	}
}
