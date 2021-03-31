/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.release.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialItem.CharArrayType;
import org.eclipse.jgit.transport.CredentialItem.InformationalMessage;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;

import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.TicketReference;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Gpg;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.data.util.Pair;
import org.springframework.data.util.Streamable;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Component to execute Git related operations.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GitOperations {

	private enum BranchCheckoutMode {
		CREATE_ONLY, CREATE_AND_UPDATE;
	}

	GitServer server = new GitServer();
	Executor executor;
	Workspace workspace;
	Logger logger;
	PluginRegistry<IssueTracker, Project> issueTracker;
	GitProperties gitProperties;
	Gpg gpg;

	/**
	 * Returns the {@link GitProject} for the given {@link Project}.
	 *
	 * @param project
	 * @return
	 */
	public GitProject getGitProject(Project project) {
		return new GitProject(project, server);
	}

	/**
	 * Resets the repositories for all modules of the given {@link Train}.
	 *
	 * @param train must not be {@literal null}.
	 */
	public void reset(TrainIteration train) {

		Assert.notNull(train, "Train must not be null!");

		ExecutionUtils.run(executor, train, module -> {
			reset(module.getProject(), Branch.from(module));
		});
	}

	public void checkout(Train train) {
		checkout(train, true);
	}

	/**
	 * Checks out all projects of the given {@link Train}.
	 *
	 * @param train
	 * @param update whether to fetch an update from origin.
	 */
	public void checkout(Train train, boolean update) {

		Assert.notNull(train, "Train must not be null!");

		if (update) {
			update(train);
		}

		AtomicBoolean masterSwitch = new AtomicBoolean();
		ExecutionUtils.run(executor, train, module -> {

			Project project = module.getProject();

			doWithGit(project, git -> {

				ModuleIteration gaIteration = train.getModuleIteration(project, Iteration.GA);
				Optional<Tag> gaTag = findTagFor(project, ArtifactVersion.of(gaIteration));

				if (!gaTag.isPresent()) {
					logger.log(project, "Checking out master branch as no GA release tag could be found!");
				}

				Branch branch = gaTag.isPresent() ? Branch.from(module) : Branch.MASTER;

				CheckoutCommand command = git.checkout().setName(branch.toString());

				if (!branchExists(project, branch)) {

					logger.log(project, "git checkout -b %s --track origin/%s", branch, branch);
					command.setCreateBranch(true)//
							.setStartPoint("origin/".concat(branch.toString()))//
							.call();
				} else {

					logger.log(project, "git checkout %s", branch);
					command.call();
				}

				reset(project, branch);
			});
		});

		if (masterSwitch.get()) {
			logger.warn(train,
					"Successfully checked out projects. There were switches to master for certain projects. This happens if the train has no branches yet.");
		} else {
			logger.log(train, "Successfully checked out projects.");
		}

	}

	/**
	 * Checks out all projects of the given {@link TrainIteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 */
	public void checkout(TrainIteration iteration) {

		Assert.notNull(iteration, "Train iteration must not be null!");

		ExecutionUtils.run(executor, iteration, module -> {

			Project project = module.getProject();
			ArtifactVersion artifactVersion = ArtifactVersion.of(module);
			Tag tag = findTagFor(project, artifactVersion).orElseThrow(() -> new IllegalStateException(
					String.format("No tag found for version %s of project %s, aborting.", artifactVersion, project)));

			doWithGit(project, git -> {

				logger.log(module, "git checkout %s", tag);
				git.checkout().setName(tag.toString()).call();
			});
		});

		logger.log(iteration, "Successfully checked out projects.");
	}

	public void prepare(TrainIteration iteration) {

		ExecutionUtils.run(executor, iteration, module -> {

			Project project = module.getProject();
			Branch branch = Branch.from(module);

			update(project);
			checkout(project, branch);

			logger.log(project, "Pulling latest updates for branch %s…", branch);

			doWithGit(project, git -> {

				logger.log(project, "git pull origin %s", branch);
				git.pull()//
						.setRebase(true)//
						.call();
			});

			logger.log(project, "Pulling updates done!", branch);
		});

		reset(iteration);
	}

	public void update(Train train) {
		ExecutionUtils.run(executor, train, module -> update(module.getProject()));
	}

	public void push(TrainIteration iteration) {
		ExecutionUtils.run(executor, iteration, this::push);
	}

	public void push(ModuleIteration module) {

		Branch branch = Branch.from(module);
		logger.log(module, "git push origin %s", branch);

		if (!branchExists(module.getProject(), branch)) {

			logger.log(module, "No branch %s in %s, skip push", branch, module.getProject().getName());
			return;
		}

		doWithGit(module.getProject(), git -> {

			Ref ref = git.getRepository().findRef(branch.toString());

			git.push()//
					.setRemote("origin")//
					.setRefSpecs(new RefSpec(ref.getName()))//
					.setCredentialsProvider(gitProperties.getCredentials())//
					.call();
		});
	}

	public void pushTags(Train train) {

		ExecutionUtils.run(executor, train.getModules(), module -> {

			logger.log(module.getProject(), "git push --tags origin");

			doWithGit(module.getProject(), git -> {

				git.push()//
						.setRemote("origin")//
						.setPushTags()//
						.setCredentialsProvider(gitProperties.getCredentials())//
						.call();
			});
		});
	}

	/**
	 * Updates the given {@link Project}. Will either pull the latest changes or clone the project's repository if not
	 * already available.
	 *
	 * @param project must not be {@literal null}.
	 */
	public void update(Project project) {

		Assert.notNull(project, "Project must not be null!");

		logger.log(project, "Updating project…");

		GitProject gitProject = new GitProject(project, server);
		String repositoryName = gitProject.getRepositoryName();

		doWithGit(project, git -> {

			if (workspace.hasProjectDirectory(project)) {

				logger.log(project, "Found existing repository %s. Obtaining latest changes…", repositoryName);

				checkout(project, Branch.MASTER);

				logger.log(project, "git fetch --tags");
				git.fetch().setTagOpt(TagOpt.FETCH_TAGS).call();

			} else {
				clone(project);
			}
		});

		logger.log(project, "Project update done!");
	}

	/**
	 * Updates the given {@link Project} by fetching all tags.
	 *
	 * @param project must not be {@literal null}.
	 */
	public void fetchTags(Project project) {

		Assert.notNull(project, "Project must not be null!");

		logger.log(project, "Updating project tags…");

		GitProject gitProject = new GitProject(project, server);
		String repositoryName = gitProject.getRepositoryName();

		doWithGit(project, git -> {

			if (workspace.hasProjectDirectory(project)) {

				logger.log(project, "Found existing repository %s. Obtaining tags…", repositoryName);
				logger.log(project, "git fetch --tags");
				git.fetch().setTagOpt(TagOpt.FETCH_TAGS).call();

			} else {
				clone(project);
			}
		});

		logger.log(project, "Project tags update done!");
	}

	public VersionTags getTags(Project project) {

		return doWithGit(project, git -> {
			return new VersionTags(project, git.tagList().call().stream()//
					.map(ref -> {

						RevCommit commit = getCommit(git.getRepository(), ref);

						PersonIdent authorIdent = commit.getAuthorIdent();
						Date authorDate = authorIdent.getWhen();
						TimeZone authorTimeZone = authorIdent.getTimeZone();
						LocalDateTime localDate = authorDate.toInstant().atZone(authorTimeZone.toZoneId()).toLocalDateTime();

						return Tag.of(ref.getName(), localDate);
					})//
					.collect(Collectors.toList()));
		});
	}

	private RevCommit getCommit(Repository repository, Ref ref) {

		return doWithGit(repository, git -> {

			Ref peeledRef = git.getRepository().getRefDatabase().peel(ref);
			LogCommand log = git.log();
			if (peeledRef.getPeeledObjectId() != null) {
				log.add(peeledRef.getPeeledObjectId());
			} else {
				log.add(ref.getObjectId());
			}

			return Streamable.of(log.call()).stream().findFirst()
					.orElseThrow(() -> new IllegalStateException("Cannot resolve commit for " + ref));
		});
	}

	/**
	 * Retrieve a list of remote branches where their related ticket is resolved.
	 *
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public TicketBranches listTicketBranches(Project project) {

		Assert.notNull(project, "Project must not be null!");

		IssueTracker tracker = issueTracker.getRequiredPluginFor(project,
				() -> String.format("No issue tracker found for project %s!", project));

		return doWithGit(project, git -> {

			update(project);

			Map<String, Branch> ticketIds = getRemoteBranches(project)//
					.filter(branch -> branch.isIssueBranch(project.getTracker()))//
					.collect(Collectors.toMap(Branch::toString, branch -> branch));

			Collection<Ticket> tickets = tracker.findTickets(project, ticketIds.keySet());

			return TicketBranches
					.from(tickets.stream().collect(Collectors.toMap(ticket -> ticketIds.get(ticket.getId()), ticket -> ticket)));
		});
	}

	/**
	 * Lookup the previous {@link TrainIteration} from existing tags.
	 *
	 * @param trainIteration must not be {@literal null}.
	 * @return
	 * @throws IllegalStateException if no previous iteration could be found.
	 */
	public TrainIteration getPreviousIteration(TrainIteration trainIteration) {

		Assert.notNull(trainIteration, "TrainIteration must not be null!");

		if (trainIteration.getIteration().isMilestone() && trainIteration.getIteration().getIterationValue() == 1) {

			Train trainToUse = getPreviousTrain(trainIteration);
			return trainToUse.getIteration(Iteration.GA);
		}

		Optional<TrainIteration> mostRecentBefore = getTags(Projects.BUILD) //
				.filter((tag, ti) -> ti.getTrain().equals(trainIteration.getTrain())) //
				.find((tag, iteration) -> iteration.getIteration().compareTo(trainIteration.getIteration()) < 0,
						Pair::getSecond);

		return mostRecentBefore.orElseThrow(() -> new IllegalStateException(
				"Cannot determine previous iteration for " + trainIteration.getReleaseTrainNameAndVersion()));
	}

	public List<TicketReference> getTicketReferencesBetween(Project project, TrainIteration from, TrainIteration to) {

		VersionTags tags = getTags(project);

		List<TicketReference> ticketReferences = doWithGit(project, git -> {

			Repository repo = git.getRepository();

			ModuleIteration toModuleIteration = to.getModule(project);
			ObjectId fromTag = resolveLowerBoundary(project, from, tags, repo);
			ObjectId toTag = resolveUpperBoundary(toModuleIteration, tags, repo);

			Iterable<RevCommit> commits = git.log().addRange(fromTag, toTag).call();

			return StreamSupport.stream(commits.spliterator(), false).flatMap(it -> {

				ParsedCommitMessage message = ParsedCommitMessage.parse(it.getFullMessage());

				if (message.getTicketReference() == null) {
					logger.warn(toModuleIteration, "Commit %s does not refer to a ticket (%s)", it.getName(),
							it.getShortMessage());
					return Stream.empty();
				}

				return Stream.of(message.getTicketReference());

			}).collect(Collectors.toList());
		});

		// make TicketReference unique
		Set<String> uniqueIds = new HashSet<>();
		List<TicketReference> uniqueTicketReferences = new ArrayList<>();

		for (TicketReference reference : ticketReferences) {
			if (uniqueIds.add(reference.getId())) {
				uniqueTicketReferences.add(reference);
			}
		}

		uniqueTicketReferences.sort(Comparator.<TicketReference> naturalOrder().reversed());

		return uniqueTicketReferences;
	}

	protected ObjectId resolveLowerBoundary(Project project, TrainIteration iteration, VersionTags tags, Repository repo)
			throws IOException {

		if (iteration.contains(project)) {

			Optional<Tag> fromTag = tags.filter(iteration.getTrain()).findTag(iteration.getIteration());

			Tag tag = fromTag.get();

			return repo.parseCommit(repo.resolve(tag.getName()));
		}

		return repo.resolve(getFirstCommit(repo));
	}

	protected ObjectId resolveUpperBoundary(ModuleIteration iteration, VersionTags tags, Repository repo)
			throws IOException {

		Optional<Tag> tag = tags.filter(iteration.getTrain()).findTag(iteration.getIteration());
		String rangeEnd = tag.map(Tag::getName).orElse(Branch.from(iteration).toString());
		return repo.parseCommit(repo.resolve(rangeEnd));
	}

	private static String getFirstCommit(Repository repo) throws IOException {

		try (RevWalk revWalk = new RevWalk(repo)) {
			return revWalk.parseCommit(repo.resolve("master")).getName();
		}
	}

	private static Train getPreviousTrain(TrainIteration trainIteration) {

		Train trainToUse = ReleaseTrains.CODD;

		for (Train train : ReleaseTrains.trains()) {
			if (train.isBefore(trainIteration.getTrain())) {
				trainToUse = train;
			} else {
				break;
			}
		}
		return trainToUse;
	}

	private Stream<Branch> getRemoteBranches(Project project) {

		return doWithGit(project, git -> {

			Collection<Ref> refs = git.lsRemote()//
					.setHeads(true)//
					.setTags(false)//
					.call();

			return refs.stream()//
					.map(Ref::getName)//
					.map(Branch::from);//
		});
	}

	/**
	 * Tags the release commits for the given {@link TrainIteration}.
	 *
	 * @param iteration
	 */
	public void tagRelease(TrainIteration iteration) {

		Assert.notNull(iteration, "Train iteration must not be null!");

		ExecutionUtils.run(executor, iteration, module -> {

			Project project = module.getProject();
			ObjectId hash = getReleaseHash(module);
			Tag tag = getTags(project).createTag(module);

			doWithGit(project, git -> {

				try (RevWalk walk = new RevWalk(git.getRepository())) {

					RevCommit commit = walk.parseCommit(hash);

					logger.log(module, "git tag %s %s", tag, hash.getName());
					git.tag().setName(tag.toString()).setObjectId(commit).call();
				}
			});
		});
	}

	/**
	 * Commits all changes currently made to all modules of the given {@link TrainIteration}. The summary can contain a
	 * single {@code %s} placeholder which the version of the current module will get replace into.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @throws Exception
	 */
	public void commit(TrainIteration iteration, String summary) {
		commit(iteration, summary, Optional.empty());
	}

	/**
	 * Commits all changes currently made to all modules of the given {@link TrainIteration}. The summary can contain a
	 * single {@code %s} placeholder which the version of the current module will get replace into.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 */
	public void commit(TrainIteration iteration, String summary, Optional<String> details) {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		ExecutionUtils.run(executor, iteration,
				module -> commit(module, expandSummary(summary, module, iteration), details));
	}

	/**
	 * Commits the given files for the given {@link ModuleIteration} using the given summary for the commit message. If no
	 * files are given, all pending changes are committed.
	 *
	 * @param module must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 */
	public void commit(ModuleIteration module, String summary) {
		commit(module, summary, Optional.empty());
	}

	/**
	 * Commits the given files for the given {@link ModuleIteration} using the given summary and details for the commit
	 * message. If no files are given, all pending changes are committed.
	 *
	 * @param module must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 */
	public void commit(ModuleIteration module, String summary, Optional<String> details) {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		Project project = module.getProject();
		IssueTracker tracker = issueTracker.getRequiredPluginFor(project,
				() -> String.format("No issue tracker found for project %s!", project));
		Ticket ticket = tracker.getReleaseTicketFor(module);

		commit(module, ticket, summary, details);
	}

	/**
	 * Commits the given files for the given {@link ModuleIteration} using the given summary and details for the commit
	 * message. If no files are given, all pending changes are committed.
	 *
	 * @param module must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 */
	public void commit(ModuleIteration module, Ticket ticket, String summary, Optional<String> details) {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		Project project = module.getProject();

		Commit commit = new Commit(ticket, summary, details);
		String author = gitProperties.getAuthor();
		String email = gitProperties.getEmail();

		logger.log(module, "git commit -m \"%s\" %s --author=\"%s <%s>\"", commit.getSummary(),
				gpg.isGpgAvailable() ? "-S" + gpg.getKeyname() : "", author, email);

		doWithGit(project, git -> {

			CommitCommand commitCommand = git.commit()//
					.setMessage(commit.toString())//
					.setAuthor(author, email)//
					.setCommitter(author, email)//
					.setAll(true);

			if (gpg.isGpgAvailable()) {
				commitCommand.setSign(true).setSigningKey(gpg.getKeyname())
						.setCredentialsProvider(new GpgPassphraseProvider(gpg));
			} else {
				commitCommand.setSign(false);
			}

			commitCommand.call();
		});
	}

	/**
	 * Checks out the given {@link Branch} of the given {@link Project}. If the given branch doesn't exist yet, a tracking
	 * branch is created assuming the branch exists in the {@code origin} remote. Pulls the latest changes from the
	 * checked out branch will be pulled to make sure we see them.
	 *
	 * @param project must not be {@literal null}.
	 * @param branch must not be {@literal null}.
	 */
	public void checkout(Project project, Branch branch) {
		checkout(project, branch, BranchCheckoutMode.CREATE_AND_UPDATE);
	}

	/**
	 * Checks out the given {@link Branch} of the given {@link Project}. If the given branch doesn't exist yet, a tracking
	 * branch is created assuming the branch exists in the {@code origin} remote. If the {@link BranchCheckoutMode} is set
	 * to {@code CREATE_AND_UPDATE} the latest changes from the checked out branch will be pulled to make sure we see
	 * them.
	 *
	 * @param project must not be {@literal null}.
	 * @param branch must not be {@literal null}.
	 * @param mode must not be {@literal null}.
	 */
	private void checkout(Project project, Branch branch, BranchCheckoutMode mode) {

		Assert.notNull(project, "Project must not be null!");
		Assert.notNull(branch, "Branch must not be null!");

		logger.log(project, "Checking out project…");

		doWithGit(project, git -> {

			Optional<Ref> ref = Optional.ofNullable(git.getRepository().findRef(branch.toString()));
			CheckoutCommand checkout = git.checkout().setName(branch.toString());

			if (ref.isPresent()) {

				logger.log(project, "git checkout %s", branch);

			} else {

				logger.log(project, "git checkout --track -b %s origin/%s", branch, branch);

				checkout.setCreateBranch(true)//
						.setUpstreamMode(SetupUpstreamMode.TRACK)//
						.setStartPoint("origin/".concat(branch.toString()));
			}

			try {
				checkout.call();
			} catch (RefNotFoundException o_O) {
				// TODO:
			}

			switch (mode) {

				case CREATE_ONLY:
					break;
				case CREATE_AND_UPDATE:
				default:
					// Pull latest changes to make sure the branch is up to date
					logger.log(project, "git pull origin %s", branch);

					git.pull()//
							.setRemote("origin")//
							.setRemoteBranchName(branch.toString())//
							.call();
					break;
			}
		});

		logger.log(project, "Checkout done!");
	}

	public void createMaintenanceBranches(TrainIteration iteration) {

		if (!iteration.getIteration().isGAIteration()) {
			return;
		}

		checkout(iteration);

		ExecutionUtils.run(executor, iteration, module -> {

			Branch branch = createMaintenanceBranch(module);
			checkout(module.getProject(), branch, BranchCheckoutMode.CREATE_ONLY);
		});
	}

	public void removeTags(TrainIteration iteration) {

		ExecutionUtils.run(executor, iteration, module -> {

			Project project = module.getProject();
			ArtifactVersion artifactVersion = ArtifactVersion.of(module);

			Optional<Tag> tag = findTagFor(project, artifactVersion);

			if (!tag.isPresent()) {
				logger.log(module, "No tag %s found project %s, skipping.", artifactVersion, project);
				return;
			}

			doWithGit(project, git -> {

				logger.log(module, "git tag -D %s", tag.get());
				git.tagDelete().setTags(tag.get().toString()).call();
			});
		});
	}

	/**
	 * Back-ports the change log created for the given {@link TrainIteration} to the given release {@link Train}s. If the
	 * {@link TrainIteration} is a service iteration itself, the master branch will become an additional port target.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param targets must not be {@literal null}.
	 */
	public void backportChangelogs(TrainIteration iteration, List<Train> targets) {

		Assert.notNull(iteration, "Train iteration must not be null!");
		Assert.notNull(targets, "Target trains must not be null!");

		ExecutionUtils.run(executor, iteration, module -> {

			BackportTargets backportTargets = new BackportTargets(module, targets);
			Project project = module.getProject();

			doWithGit(project, git -> {

				checkout(project, backportTargets.getSource());
				Optional<ObjectId> objectId = getChangelogUpdate(module);

				objectId.ifPresent(it -> backportTargets.forEach(target -> cherryPickCommitToBranch(it, project, target)));

				if (!objectId.isPresent()) {
					logger.log(project, "No changelog commit found, skipping backports!");
				}

			});
		});
	}

	/**
	 * Verify general Git operations.
	 */
	public void verify() {
		checkout(Projects.BUILD, Branch.MASTER);
	}

	private void cherryPickCommitToBranch(ObjectId id, Project project, Branch branch) {

		doWithGit(project, git -> {

			try {
				checkout(project, branch);
			} catch (RuntimeException o_O) {

				logger.warn(project, "Couldn't check out branch %s. Skipping cherrypick of commit %s.", branch, id.getName());
				return;
			}

			logger.log(project, "git cp %s", id.getName());

			// Required as the CherryPick command has no setter for a CredentialsProvide *sigh*
			if (gpg.isGpgAvailable()) {
				CredentialsProvider.setDefault(new GpgPassphraseProvider(gpg));
			}

			CherryPickResult result = git.cherryPick().include(id).call();

			if (result.getStatus().equals(CherryPickStatus.OK)) {
				logger.log(project, "Successfully cherry-picked commit %s to branch %s.", id.getName(), branch);
			} else {
				logger.warn(project, "Cherry pick failed. aborting…");
				logger.log(project, "git reset --hard");
				git.reset().setMode(ResetType.HARD).call();
			}
		});
	}

	/**
	 * Creates a version branch for the given {@link ModuleIteration}.
	 *
	 * @param module must not be {@literal null}.
	 * @return
	 */
	private Branch createMaintenanceBranch(ModuleIteration module) {

		Assert.notNull(module, "Module iteration must not be null!");

		Branch branch = Branch.from(module.getVersion());

		doWithGit(module.getProject(), git -> {
			logger.log(module, "git checkout -b %s", branch);
			git.branchCreate().setName(branch.toString()).call();
		});

		return branch;
	}

	/**
	 * Returns the {@link ObjectId} of the commit that is considered the release commit. It is identified by the summary
	 * starting with the release ticket identifier, followed by a dash separated by spaces and the key word
	 * {@code Release}. To prevent skimming through the entire Git history, we expect such a commit to be found within the
	 * 50 most recent commits.
	 *
	 * @param module
	 * @return
	 * @throws Exception
	 */
	private ObjectId getReleaseHash(ModuleIteration module) {
		return findRequiredCommit(module, "Release");
	}

	private Optional<ObjectId> getChangelogUpdate(ModuleIteration module) {
		return findCommit(module, "Updated changelog");
	}

	private ObjectId findRequiredCommit(ModuleIteration module, String summary) {

		Predicate<RevCommit> trigger = calculateFilter(module, summary);

		return findCommit(module, summary).orElseThrow(() -> new IllegalStateException(String
				.format("Did not find a commit with summary starting with '%s' for project %s", module.getProject(), trigger)));
	}

	private Optional<ObjectId> findCommit(ModuleIteration module, String summary) {
		return findCommit(module.getProject(), calculateFilter(module, summary));
	}

	private Optional<ObjectId> findCommit(Project project, Predicate<RevCommit> filter) {

		return doWithGit(project, git -> {

			for (RevCommit commit : git.log().setMaxCount(50).call()) {

				if (filter.test(commit)) {
					return Optional.of(commit.getId());
				}
			}

			return Optional.empty();
		});
	}

	private Predicate<RevCommit> calculateFilter(ModuleIteration module, String summary) {

		Project project = module.getProject();
		Ticket releaseTicket = issueTracker
				.getRequiredPluginFor(project, () -> String.format("No issue tracker found for project %s!", project))//
				.getReleaseTicketFor(module);

		return revCommit -> {

			if (revCommit.getShortMessage().contains(summary) && revCommit.getFullMessage().contains(releaseTicket.getId())) {
				return true;
			}

			return false;
		};
	}

	/**
	 * Returns the {@link Tag} that represents the {@link ArtifactVersion} of the given {@link Project}.
	 *
	 * @param project
	 * @param version
	 * @return
	 * @throws IOException
	 */
	private Optional<Tag> findTagFor(Project project, ArtifactVersion version) {

		return getTags(project).stream()//
				.filter(tag -> tag.toArtifactVersion().map(it -> it.equals(version)).orElse(false))//
				.findFirst();
	}

	private Repository getRepository(Project project) throws IOException {
		return FileRepositoryBuilder.create(workspace.getFile(".git", project));
	}

	private void clone(Project project) throws Exception {

		GitProject gitProject = getGitProject(project);

		logger.log(project, "No repository found! Cloning from %s…", gitProject.getProjectUri());

		Git git = Git.cloneRepository()//
				.setURI(gitProject.getProjectUri())//
				.setDirectory(workspace.getProjectDirectory(project))//
				.call();

		git.checkout()//
				.setName(Branch.MASTER.toString())//
				.call();

		logger.log(project, "Cloning done!", project);
	}

	private boolean branchExists(Project project, Branch branch) {

		try (Git git = new Git(getRepository(project))) {

			return git.getRepository().findRef(branch.toString()) != null;

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private void reset(Project project, Branch branch) {

		logger.log(project, "git reset --hard origin/%s", branch);

		doWithGit(project, git -> {

			git.reset()//
					.setMode(ResetType.HARD)//
					.setRef("origin/".concat(branch.toString()))//
					.call();
		});
	}

	private static String expandSummary(String summary, ModuleIteration module, TrainIteration iteration) {
		return summary.contains("%s") ? String.format(summary, module.getMediumVersionString()) : summary;
	}

	private <T> T doWithGit(Project project, GitCallback<T> callback) {

		try (Git git = new Git(getRepository(project))) {
			return callback.doWithGit(git);
		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private <T> T doWithGit(Repository repository, GitCallback<T> callback) {

		try (Git git = new Git(repository)) {
			return callback.doWithGit(git);
		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private void doWithGit(Project project, VoidGitCallback callback) {

		doWithGit(project, (GitCallback<Void>) git -> {
			callback.doWithGit(git);
			return null;
		});
	}

	private interface GitCallback<T> {
		T doWithGit(Git git) throws Exception;
	}

	private interface VoidGitCallback {
		void doWithGit(Git git) throws Exception;
	}

	/**
	 * {@link CredentialsProvider} for GPG Keys used with JGit Commit Signing.
	 */
	private static class GpgPassphraseProvider extends CredentialsProvider {

		private final Gpg gpg;

		private GpgPassphraseProvider(Gpg gpg) {
			this.gpg = gpg;
		}

		@Override
		public boolean isInteractive() {
			return false;
		}

		@Override
		public boolean supports(CredentialItem... items) {

			boolean matchesKey = matchesKey(items);
			boolean hasSettableCharArray = Arrays.stream(items).anyMatch(CharArrayType.class::isInstance);

			return matchesKey && hasSettableCharArray;
		}

		private boolean matchesKey(CredentialItem[] items) {
			return Arrays.stream(items).filter(InformationalMessage.class::isInstance) //
					.map(CredentialItem::getPromptText) //
					.map(it -> it.toLowerCase(Locale.US)) //
					.anyMatch(it -> it.contains(gpg.getKeyname().toLowerCase(Locale.US)));
		}

		@Override
		public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {

			if (!matchesKey(items)) {
				return false;
			}

			for (CredentialItem item : items) {
				if (item instanceof CharArrayType) {
					((CharArrayType) item).setValueNoCopy(gpg.getPassword().toString().toCharArray());

					return true;
				}
			}
			return false;
		}
	}

	private static class VersionedIterations {


	}
}
