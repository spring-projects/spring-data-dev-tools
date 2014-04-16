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

import java.io.File;
import java.io.IOException;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
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

	public void updatePom(TrainIteration iteration, final Phase phase) throws Exception {

		updateBomPom(iteration, phase);

		final Repository repository = new Repository(iteration.getIteration());
		final ArtifactVersion commonsVersion = iteration.getModuleVersion(COMMONS);
		final ArtifactVersion buildVersion = iteration.getModuleVersion(BUILD);

		for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

			final Project project = module.getProject();
			File pomFile = workspace.getFile(POM_XML, project);

			execute(pomFile, new PomCallback() {

				@Override
				public Pom doWith(Pom pom) {

					if (!project.equals(COMMONS)) {
						pom.setProperty(COMMONS_VERSION_PROPERTY,
								CLEANUP.equals(phase) ? commonsVersion.getNextDevelopmentVersion() : commonsVersion);
					}

					pom.setParentVersion(CLEANUP.equals(phase) ? buildVersion.getNextDevelopmentVersion() : buildVersion);
					updateRepository(pom, repository, phase);

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

		execute(bomPomFile, new PomCallback() {

			@Override
			public Pom doWith(Pom pom) {

				for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

					Artifact artifact = new Artifact(module);
					ArtifactVersion version = artifact.getVersion();
					version = PREPARE.equals(phase) ? version : version.getNextDevelopmentVersion();

					pom.setDependencyVersion(artifact.getArtifactId(), version);
				}

				return pom;
			}
		});
	}

	private void updateRepository(Pom pom, Repository repository, Phase phase) {

		if (PREPARE.equals(phase)) {
			pom.setRepositoryId(repository.getSnapshotId(), repository.getId());
			pom.setRepositoryUrl(repository.getId(), repository.getUrl());
		} else {
			pom.setRepositoryId(repository.getId(), repository.getSnapshotId());
			pom.setRepositoryUrl(repository.getSnapshotId(), repository.getSnapshotUrl());
		}
	}

	private void execute(File file, PomCallback callback) throws Exception {

		XBFileIO io = projectionFactory.io().file(file);
		Pom pom = io.read(Pom.class);

		pom = callback.doWith(pom);

		io.write(pom);
	}

	private interface PomCallback {

		public Pom doWith(Pom pom);
	}
}
