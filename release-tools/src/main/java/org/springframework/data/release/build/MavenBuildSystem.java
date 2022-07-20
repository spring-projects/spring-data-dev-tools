/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.data.release.build;

import static org.springframework.data.release.build.CommandLine.Argument.*;
import static org.springframework.data.release.model.Projects.BOM;
import static org.springframework.data.release.model.Projects.BUILD;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.release.build.CommandLine.Argument;
import org.springframework.data.release.build.CommandLine.Goal;
import org.springframework.data.release.build.Pom.Artifact;
import org.springframework.data.release.deployment.DefaultDeploymentInformation;
import org.springframework.data.release.deployment.DeploymentInformation;
import org.springframework.data.release.deployment.DeploymentProperties;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.*;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.XBProjector;
import org.xmlbeam.dom.DOMAccess;
import org.xmlbeam.io.XBStreamInput;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Greg Turnquist
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
	Gpg gpg;

	Environment env;

	static String stagingRepositoryId = null;

	static final String REPO_OPENING_TAG = "<repository>";
	static final String REPO_CLOSING_TAG = "</repository>";

	@Override
	public BuildSystem withJavaVersion(JavaVersion javaVersion) {
		return new MavenBuildSystem(workspace, projectionFactory, logger, mvn.withJavaVersion(javaVersion), properties, gpg,
				env);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#updateProjectDescriptors(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.model.TrainIteration, org.springframework.data.release.model.Phase)
	 */
	@Override
	public <M extends ProjectAware> M updateProjectDescriptors(M module, UpdateInformation information) {

		PomUpdater updater = new PomUpdater(logger, information, module.getProject());

		if (updater.isBuildProject()) {

			if (information.isBomInBuildProject()) {
				updateBom(information, "bom/pom.xml", BUILD);
			}

			updateParentPom(information);

		} else if (updater.isBomProject()) {
			updateBom(information, "bom/pom.xml", BOM);
		} else {

			doWithProjection(workspace.getFile(POM_XML, updater.getProject()), pom -> {

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
	public <M extends ProjectAware> M triggerDistributionBuild(M module) {

		Project project = module.getProject();

		if (BUILD.equals(project)) {
			return module;
		}

		if (BOM.equals(project)) {
			return module;
		}

		if (!isMavenProject(project)) {
			logger.log(project, "Skipping project as no pom.xml could be found in the working directory!");
			return module;
		}

		logger.log(project, "Triggering distribution build…");

		mvn.execute(project, CommandLine.of(Goal.CLEAN, Goal.DEPLOY, //
				SKIP_TESTS, profile("distribute"), Argument.of("-B"),
				arg("artifactory.server").withValue(properties.getServer().getUri()),
				arg("artifactory.distribution-repository").withValue(properties.getDistributionRepository()),
				arg("artifactory.username").withValue(properties.getUsername()),
				arg("artifactory.password").withValue(properties.getPassword())));

		mvn.execute(project, CommandLine.of(Goal.CLEAN, Goal.DEPLOY, //
				SKIP_TESTS, profile("distribute-schema"), Argument.of("-B"),
				arg("artifactory.server").withValue(properties.getServer().getUri()),
				arg("artifactory.distribution-repository").withValue(properties.getDistributionRepository()),
				arg("artifactory.username").withValue(properties.getUsername()),
				arg("artifactory.password").withValue(properties.getPassword())));

		logger.log(project, "Successfully finished distribution build!");

		return module;
	}

	private void updateBom(UpdateInformation updateInformation, String file, Project project) {

		TrainIteration iteration = updateInformation.getTrain();

		logger.log(BUILD, "Updating BOM pom.xml…");

		doWithProjection(workspace.getFile(file, project), pom -> {

			for (ModuleIteration module : iteration.getModulesExcept(BUILD, BOM)) {

				ArtifactVersion version = updateInformation.getProjectVersionToSet(module.getProject());

				logger.log(project, "%s", module);

				String moduleArtifactId = new MavenArtifact(module).getArtifactId();
				pom.setDependencyManagementVersion(moduleArtifactId, version);
				logger.log(project, "Updated managed dependency version for %s to %s!", moduleArtifactId, version);

				module.getProject().doWithAdditionalArtifacts(additionalArtifact -> {

					String artifactId = additionalArtifact.getArtifactId();
					Artifact artifact = pom.getManagedDependency(artifactId);

					if (artifact != null) {
						pom.setDependencyManagementVersion(artifactId, version);
						logger.log(project, "Updated managed dependency version for %s to %s!", artifactId, version);
					} else {
						logger.log(project, "Artifact %s not found, skipping update!", artifactId);
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
		doWithProjection(workspace.getFile("parent/pom.xml", BUILD), ParentPom.class, pom -> {

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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#prepareVersion(org.springframework.data.release.model.ModuleIteration, org.springframework.data.release.model.Phase)
	 */
	@Override
	public ModuleIteration prepareVersion(ModuleIteration module, Phase phase) {

		Project project = module.getProject();
		UpdateInformation information = UpdateInformation.of(module.getTrainIteration(), phase);

		CommandLine goals = CommandLine.of(Goal.goal("versions:set"), Goal.goal("versions:commit"));

		if (BOM.equals(project)) {

			mvn.execute(project, goals.and(arg("newVersion").withValue(information.getReleaseTrainVersion())) //
					.and(arg("generateBackupPoms").withValue("false")));

			mvn.execute(project, goals.and(arg("newVersion").withValue(information.getReleaseTrainVersion())) //
					.and(arg("generateBackupPoms").withValue("false")) //
					.and(arg("processAllModules").withValue("true")) //
					.and(Argument.of("-pl").withValue("bom")));

		} else {
			mvn.execute(project, goals.and(arg("newVersion").withValue(information.getProjectVersionToSet(project)))
					.and(arg("generateBackupPoms").withValue("false")));
		}

		if (BUILD.equals(project)) {

			if (!module.getTrain().usesCalver()) {
				mvn.execute(project, goals.and(arg("newVersion").withValue(information.getReleaseTrainVersion())) //
						.and(arg("generateBackupPoms").withValue("false")) //
						.and(arg("groupId").withValue("org.springframework.data")) //
						.and(arg("artifactId").withValue("spring-data-releasetrain")));
			}

			mvn.execute(project, CommandLine.of(Goal.INSTALL));
		}

		return module;
	}

	/**
	 * Perform a {@literal nexus-staging:rc-open} and extract the stagingProfileId from the results.
	 */
	@Override
	public void open() {

		try {
			CommandLine arguments = CommandLine.of(Goal.goal("nexus-staging:rc-open"), //
					profile("central"), //
					of("-s " + properties.getSettingsXml()), //
					arg("stagingProfileId").withValue(properties.getMavenCentral().getStagingProfileId()), //
					arg("openedRepositoryMessageFormat").withValue("'" + REPO_OPENING_TAG + "%s" + REPO_CLOSING_TAG + "'"));

			mvn.execute(BUILD, arguments);

			String rcOpenLogfile = "mvn-" + BUILD.getName() + "-nexus-staging.rc-open.log";

			logger.log(BUILD, "Searching " + this.workspace.getLogsDirectory().getAbsolutePath() + " for " + rcOpenLogfile);

			Path rcOpenLogfilePath = Paths.get(this.workspace.getLogsDirectory().getAbsolutePath(), rcOpenLogfile);
			logger.log(BUILD, "The log file is at " + rcOpenLogfilePath.toAbsolutePath() + " and "
					+ (rcOpenLogfilePath.toFile().exists() ? " it exists!" : " it does NOT exist!"));

			List<String> rcOpenLogContents = Files.readAllLines(rcOpenLogfilePath);

			stagingRepositoryId = rcOpenLogContents.stream() //
					.filter(line -> line.contains(REPO_OPENING_TAG) && !line.contains("%s")) //
					.reduce((first, second) -> second) // find the last entry, a.k.a. the most recent log line
					.map(s -> s.substring( //
							s.indexOf(REPO_OPENING_TAG) + REPO_OPENING_TAG.length(), //
							s.indexOf(REPO_CLOSING_TAG))) //
					.orElse("");

			logger.log(BUILD, "We just grabbed the staging repository ID at " + stagingRepositoryId);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Perform a {@literal nexus-staging:rc-close}.
	 */
	@Override
	public void close() {

		CommandLine arguments = CommandLine.of(Goal.goal("nexus-staging:rc-close"), //
				profile("central"), //
				of("-s " + properties.getSettingsXml()), //
				arg("stagingRepositoryId").withValue(stagingRepositoryId));

		mvn.execute(BUILD, arguments);
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
	public <M extends ProjectAware> M triggerBuild(M module) {

		CommandLine arguments = CommandLine.of(Goal.CLEAN, Goal.INSTALL)//
				.conditionalAnd(SKIP_TESTS, () -> module.getProject().skipTests());

		mvn.execute(module.getProject(), arguments);

		return module;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#triggerPreReleaseCheck(org.springframework.data.release.model.ModuleIteration)
	 */
	public <M extends ProjectAware> M triggerPreReleaseCheck(M module) {

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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.build.BuildSystem#verify()
	 */
	@Override
	public void verify() {

		logger.log(BUILD, "Verifying Maven Build System…");

		CommandLine arguments = CommandLine.of(Goal.CLEAN, Goal.VERIFY, //
				profile("central"), //
				SKIP_TESTS, //
				arg("gpg.executable").withValue(gpg.getExecutable()), //
				arg("gpg.passphrase").withValue(gpg.getPassphrase()), //
				arg("gpg.secretKeyring").withValue(gpg.getSecretKeyring()));

		mvn.execute(BUILD, arguments);

		mvn.execute(BUILD, CommandLine.of(Goal.goal("nexus-staging:rc-list-profiles"), //
				profile("central")));
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

		CommandLine arguments = CommandLine.of(Goal.CLEAN, Goal.DEPLOY, //
				profile("ci,release,central"), //
				SKIP_TESTS, //
				settingsXml(properties.getSettingsXml()), //
				arg("gpg.executable").withValue(gpg.getExecutable()), //
				arg("gpg.keyname").withValue(gpg.getKeyname()), //
				arg("gpg.passphrase").withValue(gpg.getPassphrase()), //
				arg("stagingRepositoryId").withValue(stagingRepositoryId)) //
				.conditionalAnd(arg("gpg.secretKeyring").withValue(gpg.getSecretKeyring()),
						() -> env.acceptsProfiles(Profiles.of("jenkins")));

		mvn.execute(module.getProject(), arguments);
	}

	private boolean isMavenProject(Project project) {
		return workspace.getFile(POM_XML, project).exists();
	}

	private void doWithProjection(File file, Consumer<Pom> callback) {
		doWithProjection(file, Pom.class, callback);
	}

	/**
	 * TODO: Move XML file callbacks using the {@link ProjectionFactory} to {@link Workspace}.
	 */
	private <T extends Pom> void doWithProjection(File file, Class<T> type, Consumer<T> callback) {

		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
			byte[] content = doWithProjection((XBProjector) projectionFactory, bis, type, callback);

			try (FileOutputStream fos = new FileOutputStream(file)) {
				fos.write(content);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static <T extends Pom> byte[] doWithProjection(XBProjector projector, InputStream stream, Class<T> type,
			Consumer<T> callback) throws IOException {

		XBStreamInput io = projector.io().stream(stream);
		T pom = io.read(type);
		callback.accept(pom);

		StringWriter writer = new StringWriter();
		try {
			projector.config().createTransformer().transform(new DOMSource(((DOMAccess) pom).getDOMNode()),
					new StreamResult(writer));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}

		String s = writer.toString();

		if (s.contains("standalone=\"no\"?><")) {
			s = s.replaceAll(Pattern.quote("standalone=\"no\"?><"), "standalone=\"no\"?>" + IOUtils.LINE_SEPARATOR + "<");
		}

		if (!s.endsWith(IOUtils.LINE_SEPARATOR)) {
			s += IOUtils.LINE_SEPARATOR;
		}

		return s.getBytes(StandardCharsets.UTF_8);
	}
}
