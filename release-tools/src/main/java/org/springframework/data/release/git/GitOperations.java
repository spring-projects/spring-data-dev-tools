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
package org.springframework.data.release.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Component to execute Git related operations.
 * 
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GitOperations {

	private enum BranchCheckoutMode {
		CREATE_ONLY, CREATE_AND_UPDATE;
	}

	GitServer server = new GitServer();
	Workspace workspace;
	Logger logger;
	PluginRegistry<IssueTracker, Project> issueTracker;
	GitProperties gitProperties;

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
	 * @throws Exception
	 */
	public void reset(TrainIteration train) {

		Assert.notNull(train, "Train must not be null!");

		ExecutionUtils.run(train, module -> {
			reset(module.getProject(), Branch.from(module));
		});
	}

	/**
	 * Checks out all projects of the given {@link TrainIteration}.
	 *
	 * @param train
	 * @throws Exception
	 */
	public void checkout(Train train) {

		Assert.notNull(train, "Train must not be null!");

		update(train);

		ExecutionUtils.run(train, module -> {

			Project project = module.getProject();

			doWithGit(project, git -> {

				Branch branch = Branch.from(module);
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

		logger.log(train, "Successfully checked out projects.");
	}

	/**
	 * Checks out all projects of the given {@link TrainIteration}.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @throws Exception
	 */
	public void checkout(TrainIteration iteration) {

		Assert.notNull(iteration, "Train iteration must not be null!");

		ExecutionUtils.run(iteration, module -> {

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

		ExecutionUtils.run(iteration, module -> {

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
		ExecutionUtils.run(train, module -> update(module.getProject()));
	}

	public void push(TrainIteration iteration) {

		ExecutionUtils.run(iteration, module -> {

			Branch branch = Branch.from(module);
			logger.log(module, "git push origin %s", branch);

			doWithGit(module.getProject(), git -> {

				Ref ref = git.getRepository().findRef(branch.toString());

				git.push()//
						.setRemote("origin")//
						.setRefSpecs(new RefSpec(ref.getName()))//
						.setCredentialsProvider(gitProperties.getCredentials())//
						.call();
			});
		});
	}

	public void pushTags(Train train) {

		ExecutionUtils.run(train.getModules(), module -> {

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
	 * @throws Exception
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

				logger.log(project, "git pull");
				git.pull().call();

			} else {
				clone(project);
			}
		});

		logger.log(project, "Project update done!");
	}

	public VersionTags getTags(Project project) {

		return doWithGit(project, git -> {
			return new VersionTags(git.tagList().call().stream()//
					.map(ref -> Tag.of(ref.getName()))//
					.collect(Collectors.toList()));
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

		IssueTracker tracker = issueTracker.getPluginFor(project);

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

		ExecutionUtils.run(iteration, module -> {

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

		ExecutionUtils.run(iteration, module -> commit(module, expandSummary(summary, module, iteration), details));
	}

	/**
	 * Commits the given files for the given {@link ModuleIteration} using the given summary for the commit message. If no
	 * files are given, all pending changes are committed.
	 *
	 * @param module must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param files can be empty.
	 */
	public void commit(ModuleIteration module, String summary, File... files) {
		commit(module, summary, Optional.empty(), files);
	}

	/**
	 * Commits the given files for the given {@link ModuleIteration} using the given summary and details for the commit
	 * message. If no files are given, all pending changes are committed.
	 * 
	 * @param module must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 * @param files can be empty.
	 * @throws Exception
	 */
	public void commit(ModuleIteration module, String summary, Optional<String> details, File... files) {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		Project project = module.getProject();
		IssueTracker tracker = issueTracker.getPluginFor(project);
		Ticket ticket = tracker.getReleaseTicketFor(module);

		Commit commit = new Commit(ticket, summary, details);
		String author = gitProperties.getAuthor();
		String email = gitProperties.getEmail();

		logger.log(module, "git commit -m \"%s\" --author=\"%s <%s>\"", commit, author, email);

		doWithGit(project, git -> {

			git.commit()//
					.setMessage(commit.toString())//
					.setAuthor(author, email)//
					.setCommitter(author, email)//
					.setAll(true)//
					.call();
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

		ExecutionUtils.run(iteration, module -> {

			Branch branch = createMaintenanceBranch(module);
			checkout(module.getProject(), branch, BranchCheckoutMode.CREATE_ONLY);
		});
	}

	public void removeTags(TrainIteration iteration) {

		ExecutionUtils.run(iteration, module -> {

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

		ExecutionUtils.run(iteration, module -> {

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

	private void cherryPickCommitToBranch(ObjectId id, Project project, Branch branch) {

		doWithGit(project, git -> {

			try {
				checkout(project, branch);
			} catch (RuntimeException o_O) {

				logger.log(project, "Couldn't check out branch %s. Skipping cherrypick of commit %s.", branch, id.getName());
				return;
			}

			logger.log(project, "git cp %s", id.getName());
			CherryPickResult result = git.cherryPick().include(id).call();

			if (result.getStatus().equals(CherryPickStatus.OK)) {
				logger.log(project, "Successfully cherry-picked commit %s to branch %s.", id.getName(), branch);
			} else {
				logger.log(project, "Cherry pick failed. aborting…");
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

		String trigger = calculateTrigger(module, summary);

		return findCommit(module, summary).orElseThrow(() -> new IllegalStateException(String
				.format("Did not find a commit with summary starting with '%s' for project %s", module.getProject(), trigger)));
	}

	private Optional<ObjectId> findCommit(ModuleIteration module, String summary) {
		return findCommitWithTrigger(module.getProject(), calculateTrigger(module, summary));
	}

	private Optional<ObjectId> findCommitWithTrigger(Project project, String trigger) {

		return doWithGit(project, git -> {

			for (RevCommit commit : git.log().setMaxCount(50).call()) {

				if (commit.getShortMessage().startsWith(trigger)) {
					return Optional.of(commit.getId());
				}
			}

			return Optional.empty();
		});
	}

	private String calculateTrigger(ModuleIteration module, String summary) {

		Project project = module.getProject();
		Ticket releaseTicket = issueTracker.getPluginFor(project).getReleaseTicketFor(module);
		return String.format("%s - %s", releaseTicket.getId(), summary);
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

	private void reset(Project project, Branch branch) throws Exception {

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
			T result = callback.doWithGit(git);
			Thread.sleep(100);
			return result;
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

	private static interface GitCallback<T> {
		T doWithGit(Git git) throws Exception;
	}

	private static interface VoidGitCallback {
		void doWithGit(Git git) throws Exception;
	}
}
