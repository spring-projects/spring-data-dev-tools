/*
 * Copyright 2020 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.build.Pom;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.data.util.Streamable;
import org.springframework.http.ResponseEntity;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.io.XBFileIO;
import org.xmlbeam.io.XBStreamInput;

/**
 * Operations for dependency management.
 *
 * @author Mark Paluch
 */
@CliComponent
@RequiredArgsConstructor
public class DependencyOperations {

	public static final Pattern REPO_MAVEN_ORG_DIR_LISTING = Pattern
			.compile("<a (?>[^>]+)>([^\\/]+)\\/<\\/a>(?>\\s*)(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})(?>\\s*)(?>-)");

	public static final DateTimeFormatter DIR_LISTING_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

	private final ProjectionFactory projectionFactory;
	private final GitOperations gitOperations;
	private final Workspace workspace;
	private final PluginRegistry<IssueTracker, Project> tracker;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	private final Logger logger;

	/**
	 * Obtain dependency upgrade proposals for {@link Project} and {@link Iteration}. Considers dependency upgrade rules
	 * according to minor/bugfix release increments. Also, SemVer with modifier are only allowed in milestone versions.
	 *
	 * @param project
	 * @param iteration
	 * @return
	 */
	public DependencyUpgradeProposals getDependencyUpgradeProposals(Project project, Iteration iteration) {

		DependencyVersions currentDependencies = getCurrentDependencies(project);
		Map<Dependency, DependencyUpgradeProposal> proposals = Collections.synchronizedMap(new LinkedHashMap<>());

		ExecutionUtils.run(executor, Streamable.of(currentDependencies.getDependencies()), dependency -> {

			DependencyVersion currentVersion = currentDependencies.get(dependency);
			List<DependencyVersion> versions = getAvailableVersions(dependency);
			DependencyUpgradeProposal proposal = getDependencyUpgradeProposal(iteration, currentVersion, versions);

			proposals.put(dependency, proposal);
		});

		return new DependencyUpgradeProposals(proposals);
	}

	/**
	 * Ensures there's a upgrade ticket for each dependency to upgrade.
	 *
	 * @param module
	 * @param dependencyVersions
	 * @return
	 */
	public Tickets createUpgradeTickets(ModuleIteration module, DependencyVersions dependencyVersions) {

		Project project = module.getProject();
		DependencyVersions upgrades = getDependencyUpgradesToApply(project, dependencyVersions);

		IssueTracker tracker = this.tracker.getRequiredPluginFor(project);
		Tickets tickets = tracker.getTicketsFor(module);

		List<Ticket> upgradeTickets = new ArrayList<>();

		upgrades.forEach((dependency, dependencyVersion) -> {

			String upgradeTicketSummary = getUpgradeTicketSummary(dependency, dependencyVersion);
			Optional<Ticket> upgradeTicket = getDependencyUpgradeTicket(tickets, upgradeTicketSummary);

			if (upgradeTicket.isPresent()) {
				logger.log(project, "Found upgrade ticket %s", upgradeTicket.get());
				upgradeTicket.ifPresent(it -> {
					tracker.assignTicketToMe(project, it);
					upgradeTickets.add(it);
				});
			} else {

				logger.log(module, "Creating upgrade ticket for %s", upgradeTicketSummary);
				Ticket ticket = tracker.createTicket(module, upgradeTicketSummary, IssueTracker.TicketType.DependencyUpgrade,
						true);
				upgradeTickets.add(ticket);
			}
		});

		// flush cache
		tracker.reset();

		return new Tickets(upgradeTickets);
	}

	/**
	 * Verifies dependencies to upgrade, applies the upgrade, creates a commit.
	 *
	 * @param tickets
	 * @param module
	 * @param dependencyVersions
	 */
	public Tickets upgradeDependencies(Tickets tickets, ModuleIteration module, DependencyVersions dependencyVersions) {

		Project project = module.getProject();
		DependencyVersions upgrades = getDependencyUpgradesToApply(project, dependencyVersions);
		ProjectDependencies dependencies = ProjectDependencies.get(project);

		if (upgrades.isEmpty()) {
			logger.log(module, "No dependency upgrades to apply");
		}

		List<Ticket> ticketsToClose = new ArrayList<>();

		upgrades.forEach((dependency, dependencyVersion) -> {

			String upgradeTicketSummary = getUpgradeTicketSummary(dependency, dependencyVersion);
			Ticket upgradeTicket = getDependencyUpgradeTicket(tickets, upgradeTicketSummary).get();
			String versionProperty = dependencies.getVersionPropertyFor(dependency);

			File pom = getPomFile(project);
			update(pom, Pom.class, it -> {
				it.setProperty(versionProperty, dependencyVersion.getIdentifier());
			});

			gitOperations.commit(module, upgradeTicket, upgradeTicketSummary, Optional.empty());

			ticketsToClose.add(upgradeTicket);
		});

		return new Tickets(ticketsToClose);
	}

	public void closeUpgradeTickets(ModuleIteration module, Tickets tickets) {

		IssueTracker tracker = this.tracker.getRequiredPluginFor(module.getProject());

		for (Ticket ticket : tickets) {
			tracker.closeTicket(module, ticket);
		}
	}

	private DependencyVersions getDependencyUpgradesToApply(Project project, DependencyVersions dependencyVersions) {

		DependencyVersions currentDependencies = getCurrentDependencies(project);
		Map<Dependency, DependencyVersion> upgrades = new LinkedHashMap<>();

		currentDependencies.forEach((dependency, dependencyVersion) -> {

			if (!dependencyVersions.hasDependency(dependency)) {
				return;
			}

			DependencyVersion upgradeVersion = dependencyVersions.get(dependency);

			if (upgradeVersion.equals(dependencyVersion)) {
				logger.log(project, "Skipping upgrade of %s (%s)", dependency.getName(), dependencyVersion.getIdentifier());
				return;
			}

			upgrades.put(dependency, upgradeVersion);
		});

		return new DependencyVersions(upgrades);
	}

	private Optional<Ticket> getDependencyUpgradeTicket(Tickets tickets, String upgradeTicketSummary) {

		List<Ticket> upgradeTickets = tickets.filter(it -> it.getSummary().equals(upgradeTicketSummary)).toList();

		if (upgradeTickets.size() > 1) {
			throw new IllegalStateException("Multiple upgrade tickets found: " + upgradeTickets);
		}

		return Optional.ofNullable(upgradeTickets.isEmpty() ? null : upgradeTickets.get(0));
	}

	protected static DependencyUpgradeProposal getDependencyUpgradeProposal(Iteration iteration,
			DependencyVersion currentVersion, List<DependencyVersion> allVersions) {

		Optional<DependencyVersion> latestMinor = findLatestMinor(iteration, currentVersion, allVersions);
		Optional<DependencyVersion> latest = findLatest(iteration, allVersions);
		List<DependencyVersion> newerVersions = allVersions.stream() //
				.sorted() //
				.filter(it -> it.compareTo(currentVersion) > 0) //
				.collect(Collectors.toList());

		DependencyVersion latestToUse = latest.filter(it -> it.isNewer(currentVersion)).orElse(currentVersion);

		return DependencyUpgradeProposal.of(iteration, currentVersion, latestMinor.orElse(latestToUse), latestToUse,
				newerVersions);
	}

	private static Optional<DependencyVersion> findLatest(Iteration iteration,
			List<DependencyVersion> availableVersions) {

		return availableVersions.stream().filter(it -> {

			if (iteration.isPublic() && StringUtils.hasText(it.getModifier())) {
				return false;
			}

			return true;

		}).max(DependencyVersion::compareTo);
	}

	private static Optional<DependencyVersion> findLatestMinor(Iteration iteration,
			DependencyVersion currentVersion,
			List<DependencyVersion> availableVersions) {

		return availableVersions.stream().filter(it -> {

			if (iteration.isPublic() && StringUtils.hasText(it.getModifier())) {
				return false;
			}

			if (it.getVersion() == null || currentVersion.getVersion() == null) {
				return false;
			}

			if (it.getTrainName() != null && currentVersion.getTrainName() != null) {
				return it.getTrainName().equals(currentVersion.getTrainName());
			}

			if (it.getVersion().getMajor() == currentVersion.getVersion().getMajor()
					&& it.getVersion().getMinor() == currentVersion.getVersion().getMinor()) {
				return true;
			}

			return false;
		}) //
				.max(DependencyVersion::compareTo);
	}

	DependencyVersions getCurrentDependencies(Project project) {

		if (!ProjectDependencies.containsProject(project)) {
			return DependencyVersions.empty();
		}

		File pom = getPomFile(project);
		ProjectDependencies dependencies = ProjectDependencies.get(project);

		return doWithPom(pom, Pom.class, it -> {

			Map<Dependency, DependencyVersion> versions = new LinkedHashMap<>();

			for (ProjectDependencies.ProjectDependency projectDependency : dependencies) {

				Dependency dependency = projectDependency.getDependency();

				if (!((project == Projects.MONGO_DB && projectDependency.getProperty().equals("mongo.reactivestreams"))
						|| project == Projects.NEO4J || project == Projects.BUILD)) {

					if (it.getDependencyVersion(dependency.getArtifactId()) == null
							&& it.getManagedDependency(dependency.getArtifactId()) == null) {
						continue;
					}
				}

				String value = it.getProperty(projectDependency.getProperty());

				if (value != null && !value.contains("${")) {
					versions.put(dependency, DependencyVersion.of(value));
				}
			}

			return new DependencyVersions(versions);
		});
	}

	private File getPomFile(Project project) {
		return workspace.getFile(project == Projects.BUILD ? "parent/pom.xml" : "pom.xml", project);
	}

	@SneakyThrows
	List<DependencyVersion> getAvailableVersions(Dependency dependency) {

		String baseUrl = String.format("https://repo1.maven.org/maven2/%s/%s/", dependency.getGroupId().replace('.', '/'),
				dependency.getArtifactId());

		ResponseEntity<byte[]> mavenMetadata = restOperations.getForEntity(baseUrl + "/maven-metadata.xml", byte[].class);
		ResponseEntity<String> directoryListing = restOperations.getForEntity(baseUrl, String.class);

		Map<String, LocalDateTime> creationDates = parseCreationDates(directoryListing.getBody());

		XBStreamInput io = projectionFactory.io().stream(new ByteArrayInputStream(mavenMetadata.getBody()));

		try {

			MavenMetadata metadata = io.read(MavenMetadata.class);

			return metadata.getVersions().stream().filter(dependency::shouldInclude).flatMap(s -> {

				try {
					return Stream.of(DependencyVersion.of(s));
				} catch (Exception e) {
					logger.log(dependency.toString(), "Cannot parse dependency version " + s);
					return Stream.empty();
				}
			}).map(it -> {

				if (creationDates.containsKey(it.getIdentifier())) {
					return it.withCreatedAt(creationDates.get(it.getIdentifier()));
				}

				return it;

			}).collect(Collectors.toList());

		} catch (Exception o_O) {
			throw new RuntimeException(String.format("Cannot determine available versions for %s", dependency), o_O);
		}
	}

	private Map<String, LocalDateTime> parseCreationDates(String body) {

		Map<String, LocalDateTime> creationDates = new LinkedHashMap<>();
		Matcher matcher = REPO_MAVEN_ORG_DIR_LISTING.matcher(body);

		while (matcher.find()) {

			String version = matcher.group(1);
			LocalDateTime creationDate = LocalDateTime.from(DIR_LISTING_TIME_FORMAT.parse(matcher.group(2)));
			creationDates.put(version, creationDate);
		}

		return creationDates;
	}

	private <T extends Pom, R> R doWithPom(File file, Class<T> type, Function<T, R> callback) {

		XBFileIO io = projectionFactory.io().file(file);

		try {

			T pom = (T) io.read(type);
			return callback.apply(pom);

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private <T extends Pom> void update(File file, Class<T> type, Consumer<T> callback) {

		XBFileIO io = projectionFactory.io().file(file);

		try {

			T pom = (T) io.read(type);
			callback.accept(pom);
			io.write(pom);

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private static String getUpgradeTicketSummary(Dependency dependency, DependencyVersion dependencyVersion) {
		return String.format("Upgrade to %s %s", dependency.getName(), dependencyVersion.getIdentifier());
	}

	public interface MavenMetadata {

		@XBRead("/metadata/versioning/versions/version/text()")
		List<String> getVersions();

	}

}
