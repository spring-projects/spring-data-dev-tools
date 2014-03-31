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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.io.CommandResult;
import org.springframework.data.release.io.OsCommandOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.stereotype.Component;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MavenOperations {

	private static final Logger LOGGER = HandlerUtils.getLogger(MavenOperations.class);
	private static final String POM_XML = "pom.xml";

	private final Workspace workspace;
	private final ProjectionFactory projectionFactory;
	private final OsCommandOperations os;

	public Pom getMavenProject(Project project) throws IOException {

		File file = workspace.getFile(POM_XML, project);
		return projectionFactory.io().file(file).read(Pom.class);
	}

	public void prepareProject(Train train, Iteration iteration, Project project) throws IOException {

		if (ReleaseTrains.BUILD.equals(project)) {
			return;
		}

		ArtifactVersion commonsVersion = train.getModuleVersion(ReleaseTrains.COMMONS, iteration);
		ArtifactVersion buildVersion = train.getModuleVersion(ReleaseTrains.BUILD, iteration);
		Repository repository = new Repository(iteration);

		File file = workspace.getFile(POM_XML, project);
		XBFileIO io = projectionFactory.io().file(file);
		Pom pom = io.read(Pom.class);

		if (!project.equals(ReleaseTrains.COMMONS)) {
			pom.setProperty("spring.data.commons", commonsVersion);
		}

		pom.setParentVersion(buildVersion);
		pom.setRepositoryId(repository.getSnapshotId(), repository.getId());
		pom.setRepositoryUrl(repository.getId(), repository.getUrl());

		io.write(pom);
	}

	/**
	 * Triggers building the distribution artifacts for all Maven projects of the given {@link Train}.
	 * 
	 * @param train
	 * @param iteration
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void triggerDistributionBuild(Train train, Iteration iteration) throws Exception {

		for (ModuleIteration moduleIteration : train.getModuleIterations(iteration)) {

			Project project = moduleIteration.getProject();

			if (ReleaseTrains.BUILD.equals(project)) {
				continue;
			}

			if (!isMavenProject(project)) {
				LOGGER.info(String.format("Skipping project %s as no pom.xml could be found in the working directory!",
						project.getFullName()));
				continue;
			}

			LOGGER.info(String.format("Triggering distribution build for %s…", project.getFullName()));

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

			LOGGER.info(String.format("Successfully finished distribution build for %s!", project));
		}
	}

	private boolean isMavenProject(Project project) {
		return workspace.getFile(POM_XML, project).exists();
	}
}
