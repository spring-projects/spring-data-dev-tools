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
package org.springframework.data.release.build;

import static org.springframework.data.release.model.Projects.*;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.release.deployment.DefaultDeploymentInformation;
import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.deployment.DeploymentProperties;
import org.springframework.data.release.deployment.DeploymentProperties.Gpg;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 */
@Component
@Order(100)
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
class MavenBuildSystem implements BuildSystem {

	private static final String POM_XML = "pom.xml";

	private final Workspace workspace;
	private final ProjectionFactory projectionFactory;
	private final Logger logger;
	private final MavenRuntime mvn;
	private final DeploymentProperties properties;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#updateProjectDescriptors(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.model.TrainIteration, org.springframework.data.release.model.Phase)
	 */
	@Override
	public ModuleIteration updateProjectDescriptors(ModuleIteration module, UpdateInformation information) {

		PomUpdater updater = new PomUpdater(logger, information, module.getProject());

		if (updater.isBuildProject()) {

			updateBom(information);
			updateParentPom(information);

		} else {

			execute(workspace.getFile(POM_XML, updater.getProject()), pom -> {

				updater.updateDependencyProperties(pom);
				updater.updateParentVersion(pom);
				updater.updateRepository(pom);
			});
		}

		return module;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#triggerDistributionBuild(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public ModuleIteration triggerDistributionBuild(ModuleIteration module) {

		Project project = module.getProject();

		if (BUILD.equals(project)) {
			return module;
		}

		if (!isMavenProject(project)) {
			logger.log(project, "Skipping project as no pom.xml could be found in the working directory!");
			return module;
		}

		logger.log(project, "Triggering distribution build…");

		ArtifactVersion version = ArtifactVersion.of(module);

		String profile = "-Pdistribute";

		if (version.isMilestoneVersion()) {
			profile = profile.concat(",milestone");
		} else if (version.isReleaseVersion()) {
			profile = profile.concat(",release");
		}

		mvn.execute(project, "clean", "deploy", "-DskipTests", profile);

		logger.log(project, "Successfully finished distribution build!");

		return module;
	}

	private void updateBom(UpdateInformation updateInformation) {

		TrainIteration iteration = updateInformation.getIteration();

		logger.log(BUILD, "Updating BOM pom.xml…");

		execute(workspace.getFile("bom/pom.xml", BUILD), pom -> {

			for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

				ArtifactVersion version = updateInformation.getProjectVersionToSet(module.getProject());

				logger.log(BUILD, "%s", module);

				pom.setDependencyManagementVersion(new MavenArtifact(module).getArtifactId(), version);

				module.getProject().doWithAdditionalArtifacts(
						additionalArtifact -> pom.setDependencyManagementVersion(additionalArtifact.getArtifactId(), version));
			}
		});
	}

	private void updateParentPom(UpdateInformation information) {

		// Fix version of shared resources to to-be-released version.
		execute(workspace.getFile("parent/pom.xml", BUILD), ParentPom.class, pom -> {

			logger.log(BUILD, "Setting shared resources version to %s.", information.getParentVersionToSet());
			pom.setSharedResourcesVersion(information.getParentVersionToSet());

			logger.log(BUILD, "Setting releasetrain property to %s.", information.getReleaseTrainVersion());
			pom.setReleaseTrain(information.getReleaseTrainVersion());
		});
	}

	public boolean isMavenProject(ModuleIteration module) {

		Project project = module.getProject();

		if (!isMavenProject(project)) {
			logger.log(module, "No pom.xml file found, skipping project.");
			return false;
		}

		return true;
	}

	/**
	 * Triggers building the distribution artifacts for all Maven projects of the given {@link Train}.
	 * 
	 * @param iteration
	 * @throws Exception
	 */
	public void triggerDistributionBuild(TrainIteration iteration) throws Exception {

		ExecutionUtils.run(iteration, module -> {

			Project project = module.getProject();

			if (BUILD.equals(project)) {
				return;
			}

			if (!isMavenProject(project)) {
				logger.log(project, "Skipping project as no pom.xml could be found in the working directory!");
				return;
			}

			logger.log(project, "Triggering distribution build…");

			ArtifactVersion version = ArtifactVersion.of(module);

			String profile = "-Pdistribute";

			if (version.isMilestoneVersion()) {
				profile = profile.concat(",milestone");
			} else if (version.isReleaseVersion()) {
				profile = profile.concat(",release");
			}

			mvn.execute(project, "clean", "deploy", "-DskipTests", profile);

			logger.log(project, "Successfully finished distribution build!");
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#prepareVersion(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.model.Phase)
	 */
	@Override
	public ModuleIteration prepareVersion(ModuleIteration module, Phase phase) {

		Project project = module.getProject();
		UpdateInformation information = UpdateInformation.of(module.getTrainIteration(), phase);

		mvn.execute(project, "versions:set", "versions:commit",
				"-DnewVersion=".concat(information.getProjectVersionToSet(project).toString()));

		if (BUILD.equals(project)) {

			mvn.execute(project, "versions:set", //
					"-DnewVersion=".concat(information.getReleaseTrainVersion()), //
					"-DgroupId=org.springframework.data", //
					"-DartifactId=spring-data-releasetrain");
		}

		return module;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#deploy(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public DeploymentInformation deploy(ModuleIteration module) {

		Assert.notNull(module, "Module must not be null!");

		DeploymentInformation information = new DefaultDeploymentInformation(module, properties);

		deployToArtifactory(module, information);
		deployToMavenCentral(module);

		return information;
	}

	/**
	 * Triggers Maven commands to deploy module artifacts to Spring Artifactory.
	 * 
	 * @param module must not be {@literal null}.
	 * @param information must not be {@literal null}.
	 */
	private void deployToArtifactory(ModuleIteration module, DeploymentInformation information) {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.notNull(information, "Deployment information must not be null!");

		logger.log(module, "Deploying artifacts to Spring Artifactory…");

		List<String> arguments = new ArrayList<>();
		arguments.add("clean");
		arguments.add("deploy");

		arguments.add("-Pci,release");
		arguments.add("-DskipTests");
		arguments.add("-Dartifactory.server=".concat(properties.getServer().getUri()));
		arguments.add("-Dartifactory.staging-repository=".concat(properties.getStagingRepository()));
		arguments.add("-Dartifactory.username=".concat(properties.getUsername()));
		arguments.add("-Dartifactory.password=".concat(properties.getPassword()));
		arguments.add("-Dartifactory.build-name=\"".concat(information.getBuildName()).concat("\""));
		arguments.add("-Dartifactory.build-number=".concat(information.getBuildNumber()));

		mvn.execute(module.getProject(), arguments);
	}

	/**
	 * Triggers Maven commands to deploy to Sonatypes OSS Nexus if the given {@link ModuleIteration} refers to a version
	 * that has to be publically released.
	 * 
	 * @param module must not be {@literal null}.
	 */
	private void deployToMavenCentral(ModuleIteration module) {

		Assert.notNull(module, "Module iteration must not be null!");

		if (!module.getIteration().isPublic()) {

			logger.log(module, "Skipping deployment to Maven Central as it's not a public version!");
			return;
		}

		logger.log(module, "Deploying artifacts to Sonatype OSS Nexus…");

		List<String> arguments = new ArrayList<>();
		arguments.add("deploy");
		arguments.add("-Pci,central");
		arguments.add("-DskipTests");

		Gpg gpg = properties.getGpg();

		arguments.add("-Dgpg.executable=".concat(gpg.getExecutable()));
		arguments.add("-Dgpg.keyname=".concat(gpg.getKeyname()));
		arguments.add("-Dgpg.password=".concat(gpg.getPassword()));

		mvn.execute(module.getProject(), arguments);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return isMavenProject(project);
	}

	private boolean isMavenProject(Project project) {
		return workspace.getFile(POM_XML, project).exists();
	}

	private void execute(File file, Consumer<Pom> callback) {
		execute(file, Pom.class, callback);
	}

	/**
	 * TODO: Move XML file callbacks using the {@link ProjectionFactory} to {@link Workspace}.
	 */
	private <T extends Pom> void execute(File file, Class<T> type, Consumer<T> callback) {

		XBFileIO io = projectionFactory.io().file(file);

		try {

			T pom = (T) io.read(type);
			callback.accept(pom);
			io.write(pom);

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}
}
