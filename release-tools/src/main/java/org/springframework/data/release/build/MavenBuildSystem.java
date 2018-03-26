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

import static org.springframework.data.release.build.CommandLine.Argument.*;
import static org.springframework.data.release.build.CommandLine.Goal.*;
import static org.springframework.data.release.model.Projects.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.core.annotation.Order;
import org.springframework.data.release.build.CommandLine.Argument;
import org.springframework.data.release.build.CommandLine.Goal;
import org.springframework.data.release.build.Pom.Artifact;
import org.springframework.data.release.deployment.DefaultDeploymentInformation;
import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.deployment.DeploymentProperties;
import org.springframework.data.release.deployment.DeploymentProperties.Gpg;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Module;
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
 * @author Mark Paluch
 */
@Component
@Order(100)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class MavenBuildSystem implements BuildSystem {

	static String POM_XML = "pom.xml";

	Workspace workspace;
	ProjectionFactory projectionFactory;
	Logger logger;
	MavenRuntime mvn;
	DeploymentProperties properties;

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
	 * @see org.springframework.data.release.build.BuildSystem#triggerDistributionBuild(org.springframework.data.release.model.Module)
	 */
	@Override
	public Module triggerDistributionBuild(Module module) {

		Project project = module.getProject();

		if (BUILD.equals(project)) {
			return module;
		}

		if (!isMavenProject(project)) {
			logger.log(project, "Skipping project as no pom.xml could be found in the working directory!");
			return module;
		}

		logger.log(project, "Triggering distribution build…");

		mvn.execute(project, CommandLine.of(Goal.CLEAN, Goal.DEPLOY, //
				SKIP_TESTS, profile("distribute"), Argument.of("-B")));

		logger.log(project, "Successfully finished distribution build!");

		return module;
	}

	private void updateBom(UpdateInformation updateInformation) {

		TrainIteration iteration = updateInformation.getTrain();

		logger.log(BUILD, "Updating BOM pom.xml…");

		execute(workspace.getFile("bom/pom.xml", BUILD), pom -> {

			for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

				ArtifactVersion version = updateInformation.getProjectVersionToSet(module.getProject());

				logger.log(BUILD, "%s", module);

				String moduleArtifactId = new MavenArtifact(module).getArtifactId();
				pom.setDependencyManagementVersion(moduleArtifactId, version);
				logger.log(BUILD, "Updated managed dependency version for %s to %s!", moduleArtifactId, version);

				module.getProject().doWithAdditionalArtifacts(additionalArtifact -> {

					String artifactId = additionalArtifact.getArtifactId();
					Artifact artifact = pom.getManagedDependency(artifactId);

					if (artifact != null) {
						pom.setDependencyManagementVersion(artifactId, version);
						logger.log(BUILD, "Updated managed dependency version for %s to %s!", artifactId, version);
					} else {
						logger.log(BUILD, "Artifact %s not found, skipping update!", artifactId);
					}
				});
			}

			if (updateInformation.getPhase().equals(Phase.PREPARE)) {

				// Make sure we have no snapshot leftovers
				List<Artifact> snapshotDependencies = pom.getSnapshotDependencies();

				if (!snapshotDependencies.isEmpty()) {
					throw new IllegalStateException(String.format("Found snapshot dependencies %s!", snapshotDependencies));
				}
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

			mvn.execute(project,
					CommandLine.of(Goal.CLEAN, Goal.DEPLOY, ReleaseVersion.of(module).getDistributionProfiles(), SKIP_TESTS));

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

		CommandLine goals = CommandLine.of(goal("versions:set"), goal("versions:commit"));

		mvn.execute(project, goals.and(arg("newVersion").withValue(information.getProjectVersionToSet(project))));

		if (BUILD.equals(project)) {

			mvn.execute(project, goals.and(arg("newVersion").withValue(information.getReleaseTrainVersion()))//
					.and(arg("groupId").withValue("org.springframework.data"))//
					.and(arg("artifactId").withValue("spring-data-releasetrain")));

			mvn.execute(project, CommandLine.of(Goal.INSTALL));
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#triggerBuild(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public ModuleIteration triggerBuild(ModuleIteration module) {

		CommandLine arguments = CommandLine.of(Goal.CLEAN, Goal.INSTALL)//
				.conditionalAnd(SKIP_TESTS, () -> module.getProject().skipTests());

		mvn.execute(module.getProject(), arguments);

		return module;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#triggerPreReleaseCheck(org.springframework.data.release.model.ModuleIteration)
	 */
	public ModuleIteration triggerPreReleaseCheck(ModuleIteration module) {

		mvn.execute(module.getProject(), CommandLine.of(Goal.CLEAN, Goal.VALIDATE, profile("pre-release")));

		return module;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return isMavenProject(project);
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

		if (!module.getIteration().isPreview()) {
			logger.log(module, "Not a preview version (milestone or release candidate). Skipping Artifactory deployment.");
			return;
		}

		logger.log(module, "Deploying artifacts to Spring Artifactory…");

		CommandLine arguments = CommandLine.of(Goal.CLEAN, Goal.DEPLOY, //
				profile("ci,release,artifactory"), //
				SKIP_TESTS, //
				arg("artifactory.server").withValue(properties.getServer().getUri()),
				arg("artifactory.staging-repository").withValue(properties.getStagingRepository()),
				arg("artifactory.username").withValue(properties.getUsername()),
				arg("artifactory.password").withValue(properties.getPassword()),
				arg("artifactory.build-name").withQuotedValue(information.getBuildName()),
				arg("artifactory.build-number").withValue(information.getBuildNumber()));

		mvn.execute(module.getProject(), arguments);
	}

	/**
	 * Triggers Maven commands to deploy to Sonatype's OSS Nexus if the given {@link ModuleIteration} refers to a version
	 * that has to be publicly released.
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

		Gpg gpg = properties.getGpg();

		CommandLine arguments = CommandLine.of(Goal.DEPLOY, //
				profile("ci,release,central"), //
				SKIP_TESTS, //
				arg("gpg.executable").withValue(gpg.getExecutable()), //
				arg("gpg.keyname").withValue(gpg.getKeyname()), //
				arg("gpg.password").withValue(gpg.getPassword()));

		mvn.execute(module.getProject(), arguments);
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

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static class ReleaseVersion {

		private final ArtifactVersion version;

		/**
		 * Creates a new {@link ReleaseVersion} for the given {@link ModuleIteration}.
		 *
		 * @param module must not be {@literal null}.
		 * @return
		 */
		public static ReleaseVersion of(ModuleIteration module) {

			ArtifactVersion artifactVersion = ArtifactVersion.of(module);

			Assert.isTrue(artifactVersion.isMilestoneVersion() || artifactVersion.isReleaseVersion(),
					String.format("Given module is not in a fixed version, detected %s!", artifactVersion));

			return new ReleaseVersion(ArtifactVersion.of(module));
		}

		/**
		 * Returns the Maven profiles to be used during the distribution build.
		 *
		 * @return
		 */
		public Argument getDistributionProfiles() {

			if (version.isMilestoneVersion()) {
				return profile("distribute", "milestone");
			} else if (version.isReleaseVersion()) {
				return profile("distribute", "release");
			}

			throw new IllegalStateException("Should not occur!");
		}
	}
}
