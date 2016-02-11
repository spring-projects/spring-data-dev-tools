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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.jira.IssueTracker;
import org.springframework.data.release.jira.Ticket;
import org.springframework.data.release.jira.TicketBranches;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class GitOperations {

	private final GitServer server = new GitServer();
	private final Workspace workspace;
	private final Logger logger;
	private final PluginRegistry<IssueTracker, Project> issueTracker;
	private final GitProperties gitProperties;

	public GitProject getGitProject(Project project) {
		return new GitProject(project, server);
	}

	/**
	 * Resets the repositories for all modules of the given {@link Train}.
	 * 
	 * @param train must not be {@literal null}.
	 * @throws Exception
	 */
	public void reset(TrainIteration train) throws Exception {

		Assert.notNull(train, "Train must not be null!");

		ExecutionUtils.run(train, module -> {

			Branch branch = Branch.from(module);

			try (Git git = new Git(getRepository(module.getProject()))) {

				logger.log(module, "git reset --hard origin/%s", branch);

				reset(git, branch.toString());
			}
		});
	}

	/**
	 * Checks out all projects of the given {@link TrainIteration}.
	 *
	 * @param train
	 * @throws Exception
	 */
	public void checkout(Train train) {

		update(train);

		ExecutionUtils.run(train, module -> {

			try (Git git = new Git(getRepository(module.getProject()))) {

				Branch branch = Branch.from(module);
				CheckoutCommand command = git.checkout().setName(branch.toString());

				if (!branchExists(module.getProject(), branch)) {

					logger.log(module.getProject(), "git checkout -b %s --track origin/%s", branch, branch);
					command.setCreateBranch(true).//
							setStartPoint("origin/".concat(branch.toString())).//
							call();

				} else {

					logger.log(module.getProject(), "git checkout %s", branch);
					command.call();
				}

				logger.log(module.getProject(), "git reset --hard origin/%s", branch);
				reset(git, branch.toString());
			}
		});

		logger.log(train, "Successfully checked out projects.");
	}

	/**
	 * Checks out all projects of the given {@link TrainIteration}.
	 * 
	 * @param iteration
	 * @throws Exception
	 */
	public void checkout(TrainIteration iteration) {

		update(iteration.getTrain());

		ExecutionUtils.run(iteration, module -> {

			Project project = module.getProject();
			ArtifactVersion artifactVersion = ArtifactVersion.of(module);

			Tag tag = findTagFor(project, artifactVersion);

			if (tag == null) {
				throw new IllegalStateException(
						String.format("No tag found for version %s of project %s, aborting.", artifactVersion, project));
			}

			try (Git git = new Git(getRepository(module.getProject()))) {

				logger.log(module, "git checkout %s", tag);

				git.checkout().setStartPoint(tag.toString()).call();
			}
		});

		logger.log(iteration, "Successfully checked out projects.");
	}

	public void prepare(TrainIteration iteration) throws Exception {

		ExecutionUtils.run(iteration, module -> {

			Branch branch = Branch.from(module);

			update(module.getProject());

			logger.log(module.getProject(), "git checkout %s && git pull origin %s", branch, branch);
			checkout(module.getProject(), branch);

			try (Git git = new Git(getRepository(module.getProject()))) {
				git.pull().//
						setRebase(true).//
						call();
			}
		});
	}

	public void update(Train train) {
		ExecutionUtils.run(train, module -> update(module.getProject()));
	}

	public void push(TrainIteration iteration) {

		ExecutionUtils.run(iteration, module -> {

			Branch branch = Branch.from(module);
			logger.log(module, "git push origin %s", branch);

			try (Git git = new Git(getRepository(module.getProject()))) {

				Ref ref = git.getRepository().getRef(branch.toString());

				git.push().//
						setRemote("origin").//
						setRefSpecs(new RefSpec(ref.getName())).//
						setCredentialsProvider(gitProperties.getCredentials()).//
						call();
			}
		});
	}

	public void pushTags(Train train) {

		ExecutionUtils.run(train.getModules(), module -> {

			logger.log(module.getProject(), "git push --tags");

			try (Git git = new Git(getRepository(module.getProject()))) {

				git.push().//
						setRemote("origin").//
						setPushTags().//
						setCredentialsProvider(gitProperties.getCredentials()).//
						call();
			}
		});
	}

	public void update(Project project) throws Exception {

		GitProject gitProject = new GitProject(project, server);
		String repositoryName = gitProject.getRepositoryName();

		try (Git git = new Git(getRepository(project))) {

			if (workspace.hasProjectDirectory(project)) {

				logger.log(project, "Found existing repository %s. Obtaining latest changes…", repositoryName);
				logger.log(project,
						"git checkout master && git reset --hard origin/master && git fetch --tags && git pull origin master");

				checkout(project, Branch.MASTER);

				reset(git, "master");
				git.fetch().setTagOpt(TagOpt.FETCH_TAGS).call();
				git.pull().call();

			} else {

				logger.log(project, "No repository found! Cloning from %s…", gitProject.getProjectUri());

				clone(project);
			}
		}
	}

	public VersionTags getTags(Project project) {

		try (Git git = new Git(getRepository(project))) {

			return new VersionTags(git.tagList().call().stream().//
					map(ref -> Tag.of(ref.getName())).//
					collect(Collectors.toList()));

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	/**
	 * Retrieve a list of remote branches where their related ticket is resolved.
	 * 
	 * @param project
	 * @return
	 */
	public TicketBranches listTicketBranches(Project project) {

		IssueTracker tracker = issueTracker.getPluginFor(project);

		try (Git git = new Git(getRepository(project))) {
			update(project);
			Pattern pattern = Pattern.compile(project.getTracker().getTicketPattern());

			Collection<Ref> branches = git.lsRemote().setHeads(true).setTags(false).call();

			Set<String> possibleTicketIds = branches.stream().//
					filter(branch -> pattern.matcher(branch.getName()).find()).//
					map(branch -> {
						Matcher matcher = pattern.matcher(branch.getName());
						matcher.find();
						return matcher.group(1);
					}).//
					collect(Collectors.toSet());

			Collection<Ticket> tickets = tracker.findTickets(project, possibleTicketIds);
			Map<String, Ticket> ticketMap = tickets.stream().collect(Collectors.toMap(Ticket::getId, ticket -> ticket));

			Map<Branch, Ticket> ticketBranches = new HashMap<>();
			branches.stream().//
					map(branch -> {
						if (branch.getName().startsWith(Constants.R_HEADS)) {
							return branch.getName().substring(Constants.R_HEADS.length());
						}

						if (branch.getName().startsWith(Constants.R_REMOTES)) {
							return branch.getName().substring(Constants.R_REMOTES.length());
						}
						return branch.getName();
					}).filter(branchName -> {
						Matcher matcher = pattern.matcher(branchName);
						return matcher.find();
					}).//
					forEach((branchName) -> {

						Matcher matcher = pattern.matcher(branchName);
						matcher.find();
						String ticketId = matcher.group(1);
						ticketBranches.put(Branch.from(branchName), ticketMap.get(ticketId));
					});

			return TicketBranches.from(ticketBranches);
		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	public void tagRelease(TrainIteration iteration) {

		ExecutionUtils.run(iteration, module -> {

			Branch branch = Branch.from(module);
			Project project = module.getProject();

			try (Git git = new Git(getRepository(module.getProject()))) {

				logger.log(module, "git checkout %s", branch);
				checkout(project, branch);

				logger.log(module, "git pull origin %s", branch);
				git.pull().call();

				ObjectId hash = getReleaseHash(module);
				Tag tag = getTags(project).createTag(module);

				try (RevWalk walk = new RevWalk(git.getRepository())) {

					RevCommit commit = walk.parseCommit(hash);

					logger.log(module, "git tag %s %s", tag, hash.getName());
					git.tag().setName(tag.toString()).setObjectId(commit).call();
				}
			}
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
	public void commit(TrainIteration iteration, String summary) throws Exception {
		commit(iteration, summary, Optional.empty());
	}

	/**
	 * Commits all changes currently made to all modules of the given {@link TrainIteration}. The summary can contain a
	 * single {@code %s} placeholder which the version of the current module will get replace into.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @param summary must not be {@literal null} or empty.
	 * @param details can be {@literal null} or empty.
	 * @throws Exception
	 */
	public void commit(TrainIteration iteration, String summary, Optional<String> details) throws Exception {

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
	 * @throws Exception
	 */
	public void commit(ModuleIteration module, String summary, File... files) throws Exception {
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
	public void commit(ModuleIteration module, String summary, Optional<String> details, File... files) throws Exception {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.hasText(summary, "Summary must not be null or empty!");

		Project project = module.getProject();
		IssueTracker tracker = issueTracker.getPluginFor(project);
		Ticket ticket = tracker.getReleaseTicketFor(module);

		Commit commit = new Commit(ticket, summary, details);
		String author = gitProperties.getAuthor();
		String email = gitProperties.getEmail();

		logger.log(module, "git commit -m \"%s\" --author=\"%s <%s>\"", commit, author, email);

		try (Git git = new Git(getRepository(module.getProject()))) {

			git.commit().//
					setMessage(commit.toString()).//
					setAuthor(author, email).//
					setAll(true).//
					call();
		}
	}

	public void checkout(Project project, Branch branch) throws Exception {

		try (Git git = new Git(getRepository(project))) {

			Ref ref = git.getRepository().getRef(branch.toString());
			CheckoutCommand checkout = git.checkout().setName(branch.toString());

			if (ref == null) {

				checkout.setCreateBranch(true).//
						setUpstreamMode(SetupUpstreamMode.TRACK).//
						setStartPoint("origin/".concat(branch.toString()));
			}

			checkout.call();
		}
	}

	public void createMaintenanceBranches(TrainIteration iteration) throws Exception {

		checkout(iteration);

		ExecutionUtils.run(iteration, module -> {
			Branch branch = createMaintenanceBranch(module);
			checkout(module.getProject(), branch);
		});
	}

	private Branch createMaintenanceBranch(ModuleIteration module) throws Exception {

		try (Git git = new Git(getRepository(module.getProject()))) {

			Branch branch = Branch.from(module);
			git.branchCreate().setName(branch.toString()).call();

			return branch;
		}
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
	private ObjectId getReleaseHash(ModuleIteration module) throws Exception {

		Project project = module.getProject();

		Ticket releaseTicket = issueTracker.getPluginFor(project).getReleaseTicketFor(module);
		String trigger = String.format("%s - Release", releaseTicket.getId());

		try (Git git = new Git(getRepository(module.getProject()))) {

			for (RevCommit commit : git.log().setMaxCount(50).call()) {

				if (commit.getShortMessage().startsWith(trigger)) {
					return commit.getId();
				}
			}
		}

		throw new IllegalStateException(
				String.format("Did not find a release commit for project %s (ticket id %s)", project, releaseTicket.getId()));
	}

	/**
	 * Returns the {@link Tag} that represents the {@link ArtifactVersion} of the given {@link Project}.
	 * 
	 * @param project
	 * @param version
	 * @return
	 * @throws IOException
	 */
	private Tag findTagFor(Project project, ArtifactVersion version) {

		return getTags(project).stream().//
				filter(tag -> tag.toArtifactVersion().map(it -> it.equals(version)).orElse(false)).//
				findFirst().orElseThrow(() -> new IllegalArgumentException(
						String.format("No tag found for version %s of project %s!", version, project)));
	}

	private Repository getRepository(Project project) throws IOException {
		return FileRepositoryBuilder.create(workspace.getFile(".git", project));
	}

	private void clone(Project project) throws Exception {

		Git git = Git.cloneRepository().//
				setURI(getGitProject(project).getProjectUri()).//
				setDirectory(workspace.getProjectDirectory(project)).//
				call();

		git.checkout().setName(Branch.MASTER.toString()).//
				call();
	}

	private boolean branchExists(Project project, Branch branch) {

		try (Git git = new Git(getRepository(project))) {

			return git.getRepository().getRef(branch.toString()) != null;

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private static void reset(Git git, String name) throws Exception {
		git.reset().setMode(ResetType.HARD).setRef("origin/".concat(name)).call();
	}

	private static String expandSummary(String summary, ModuleIteration module, TrainIteration iteration) {
		return summary.contains("%s") ? String.format(summary, module.getMediumVersionString()) : summary;
	}
}
