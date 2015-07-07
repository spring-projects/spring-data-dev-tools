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
package org.springframework.data.release.maven;

import static org.springframework.data.release.model.Phase.*;
import static org.springframework.data.release.model.Projects.*;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.release.io.CommandResult;
import org.springframework.data.release.io.OsCommandOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class MavenOperations {

	private static final String COMMONS_VERSION_PROPERTY = "springdata.commons";
	private static final String POM_XML = "pom.xml";

	private final Workspace workspace;
	private final ProjectionFactory projectionFactory;
	private final OsCommandOperations os;
	private final Logger logger;

	public Pom getMavenProject(Project project) throws IOException {

		File file = workspace.getFile(POM_XML, project);
		return projectionFactory.io().file(file).read(Pom.class);
	}

	/**
	 * Updates the POM files for all Maven projects contained in the iteration:
	 * <ol>
	 * <li>Updates the BOM POM.</li>
	 * <li>Updates the dependency version to Spring Data Commons to the current release version for all projects depending
	 * on it.</li>
	 * <li>Switches to the Spring release Maven repository.</li>
	 * </ol>
	 * If {@link Phase} is {@link Phase#CLEANUP} the changes will be rolled back.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @param phase must not be {@literal null}.
	 * @throws Exception
	 */
	public void updatePom(TrainIteration iteration, final Phase phase) throws Exception {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Assert.notNull(phase, "Phase must not be null!");

		updateBomPom(iteration, phase);

		final Repository repository = new Repository(iteration.getIteration());
		final ArtifactVersion buildVersion = iteration.getModuleVersion(BUILD);
		final ArtifactVersion nextBuildVersion = buildVersion.getNextDevelopmentVersion();

		// Fix version of shared resources to to-be-released version.
		execute(workspace.getFile("parent/pom.xml", BUILD), new PomCallback<ParentPom>() {

			@Override
			public ParentPom doWith(ParentPom pom) {
				pom.setSharedResourcesVersion(phase.equals(PREPARE) ? buildVersion : nextBuildVersion);
				return pom;
			}
		});

		for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

			final Project project = module.getProject();

			if (!isMavenProject(project)) {
				logger.log(module, "No pom.xml file found, skipping project.");
				continue;
			}

			execute(workspace.getFile(POM_XML, project), new PomCallback<Pom>() {

				@Override
				public Pom doWith(Pom pom) {

					for (Project dependency : project.getDependencies()) {

						String dependencyProperty = dependency.getDependencyProperty();

						if (pom.getProperty(dependencyProperty) == null) {
							continue;
						}

						ArtifactVersion dependencyVersion = iteration.getModuleVersion(dependency);
						ArtifactVersion version = CLEANUP.equals(phase) ? dependencyVersion.getNextDevelopmentVersion()
								: dependencyVersion;

						logger.log(project, "Updating %s dependency version property %s to %s.", dependency.getFullName(),
								dependencyProperty, version);
						pom.setProperty(dependencyProperty, version);
					}

					ArtifactVersion version = CLEANUP.equals(phase) ? nextBuildVersion : buildVersion;
					logger.log(project, "Updating Spring Data Build Parent version to %s.", version);
					pom.setParentVersion(version);

					updateRepository(project, pom, repository, phase);

					return pom;
				}
			});
		}
	}

	/**
	 * Triggers building the distribution artifacts for all Maven projects of the given {@link Train}.
	 * 
	 * @param train
	 * @param iteration
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void triggerDistributionBuild(TrainIteration iteration) throws Exception {

		for (ModuleIteration moduleIteration : iteration) {

			Project project = moduleIteration.getProject();

			if (BUILD.equals(project)) {
				continue;
			}

			if (!isMavenProject(project)) {
				logger.log(project, "Skipping project as no pom.xml could be found in the working directory!");
				continue;
			}

			logger.log(project, "Triggering distribution build…");

			ArtifactVersion version = ArtifactVersion.from(moduleIteration);

			String command = "mvn clean deploy -DskipTests -Pdistribute";

			if (version.isMilestoneVersion()) {
				command = command.concat(",milestone");
			} else if (version.isReleaseVersion()) {
				command = command.concat(",release");
			}

			CommandResult result = os.executeWithOutput(command, moduleIteration.getProject()).get();

			if (result.hasError()) {
				throw result.getException();
			}

			logger.log(project, "Successfully finished distribution build!");
		}
	}

	private boolean isMavenProject(Project project) {
		return workspace.getFile(POM_XML, project).exists();
	}

	private void updateBomPom(final TrainIteration iteration, final Phase phase) throws Exception {

		File bomPomFile = workspace.getFile("bom/pom.xml", BUILD);

		logger.log(BUILD, "Updating BOM pom.xml…");

		execute(bomPomFile, new PomCallback<Pom>() {

			@Override
			public Pom doWith(Pom pom) {

				for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

					Artifact artifact = new Artifact(module);
					ArtifactVersion version = artifact.getVersion();
					version = PREPARE.equals(phase) ? version : version.getNextDevelopmentVersion();

					logger.log(BUILD, "%s", module);

					pom.setDependencyManagementVersion(artifact.getArtifactId(), version);

					for (String additionalArtifact : module.getProject().getAdditionalArtifacts()) {
						pom.setDependencyManagementVersion(additionalArtifact, version);
					}
				}

				return pom;
			}
		});
	}

	private void updateRepository(Project project, Pom pom, Repository repository, Phase phase) {

		String message = "Switching to Spring repository %s (%s).";

		if (PREPARE.equals(phase)) {

			logger.log(project, message, repository.getId(), repository.getUrl());

			pom.setRepositoryId(repository.getSnapshotId(), repository.getId());
			pom.setRepositoryUrl(repository.getId(), repository.getUrl());

		} else {

			logger.log(project, message, repository.getSnapshotId(), repository.getSnapshotUrl());

			pom.setRepositoryId(repository.getId(), repository.getSnapshotId());
			pom.setRepositoryUrl(repository.getSnapshotId(), repository.getSnapshotUrl());
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Pom> void execute(File file, PomCallback<T> callback) throws Exception {

		XBFileIO io = projectionFactory.io().file(file);
		Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(callback.getClass(), PomCallback.class);

		T pom = (T) io.read(typeArgument);
		pom = callback.doWith(pom);

		io.write(pom);
	}

	private interface PomCallback<T extends Pom> {

		public T doWith(T pom);
	}
}
