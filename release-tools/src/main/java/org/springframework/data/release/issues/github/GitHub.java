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
package org.springframework.data.release.issues.github;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.git.Tag;
import org.springframework.data.release.git.VersionTags;
import org.springframework.data.release.issues.Changelog;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.TicketReference;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.DocumentationMetadata;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Component
class GitHub extends GitHubSupport implements IssueTracker {

	private static final String MILESTONE_URI = "/repos/spring-projects/{repoName}/milestones?state={state}";
	private static final String ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE = "/repos/spring-projects/{repoName}/issues?milestone={id}&state=all&assignee={assignee}";
	private static final String ISSUES_BY_MILESTONE_URI_TEMPLATE = "/repos/spring-projects/{repoName}/issues?milestone={id}&state=all";
	private static final String MILESTONES_URI_TEMPLATE = "/repos/spring-projects/{repoName}/milestones";
	private static final String MILESTONE_BY_ID_URI_TEMPLATE = "/repos/spring-projects/{repoName}/milestones/{id}";
	private static final String ISSUE_BY_ID_URI_TEMPLATE = "/repos/spring-projects/{repoName}/issues/{id}";
	private static final String ISSUES_URI_TEMPLATE = "/repos/spring-projects/{repoName}/issues";
	private static final String RELEASE_BY_TAG_URI_TEMPLATE = "/repos/spring-projects/{repoName}/releases/tags/{tag}";
	private static final String RELEASE_URI_TEMPLATE = "/repos/spring-projects/{repoName}/releases";
	private static final String RELEASE_BY_ID_URI_TEMPLATE = "/repos/spring-projects/{repoName}/releases/{id}";

	private static final ParameterizedTypeReference<List<Milestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<Milestone>>() {};
	private static final ParameterizedTypeReference<List<GitHubReadIssue>> ISSUES_TYPE = new ParameterizedTypeReference<List<GitHubReadIssue>>() {};
	private static final ParameterizedTypeReference<GitHubReadIssue> ISSUE_TYPE = new ParameterizedTypeReference<GitHubReadIssue>() {};

	private static final Map<TicketType, Label> TICKET_LABELS = new HashMap<>();

	static {

		TICKET_LABELS.put(TicketType.Task, LabelConfiguration.TYPE_TASK);
		TICKET_LABELS.put(TicketType.DependencyUpgrade, LabelConfiguration.TYPE_DEPENDENCY_UPGRADE);
	}

	private final Logger logger;
	private final GitHubProperties properties;
	private final ExecutorService executorService;


	public GitHub(@Qualifier("tracker") RestTemplateBuilder templateBuilder, Logger logger, GitHubProperties properties,
			ExecutorService executorService) {

		super(createOperations(templateBuilder, properties));
		this.logger = logger;
		this.properties = properties;
		this.executorService = executorService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.JiraConnector#flushTickets()
	 */
	@Override
	@CacheEvict(value = { "tickets", "release-tickets", "milestone" }, allEntries = true)
	public void reset() {

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getReleaseTicketFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("release-tickets")
	public Ticket getReleaseTicketFor(ModuleIteration module) {
		return getTicketsFor(module).getReleaseTicket(module);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IssueTracker#findTickets(Project, Collection)
	 */
	@Override
	@Cacheable("tickets")
	public Collection<Ticket> findTickets(Project project, Collection<String> ticketIds) {

		String repositoryName = GitProject.of(project).getRepositoryName();
		List<Ticket> tickets = new ArrayList<>();

		ticketIds.forEach(ticketId -> {

			GitHubReadIssue ticket = findTicket(repositoryName, ticketId);
			if (ticket != null) {
				tickets.add(toTicket(ticket));
			}
		});

		return tickets;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#getChangelogFor(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	@Cacheable("changelogs")
	public Changelog getChangelogFor(ModuleIteration moduleIteration) {

		Tickets tickets = getIssuesFor(moduleIteration, false, false).//
				map(issue -> toTicket(issue)).//
				collect(Tickets.toTicketsCollector());

		logger.log(moduleIteration, "Created changelog with %s entries.", tickets.getOverallTotal());

		return Changelog.of(moduleIteration, tickets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.plugin.core.Plugin#supports(java.lang.Object)
	 */
	@Override
	public boolean supports(Project project) {
		return project.uses(Tracker.GITHUB);
	}

	@Override
	public Tickets getTicketsFor(TrainIteration iteration) {
		return getTicketsFor(iteration, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.GitHubIssueConnector#getTicketsFor(org.springframework.data.release.model.TrainIteration, boolean)
	 */
	@Override
	public Tickets getTicketsFor(TrainIteration trainIteration, boolean forCurrentUser) {

		if (forCurrentUser) {
			logger.log(trainIteration, "Retrieving tickets (for user %s)…", properties.getUsername());
		} else {
			logger.log(trainIteration, "Retrieving tickets…");
		}

		Tickets tickets = trainIteration.stream(). //
				filter(moduleIteration -> supports(moduleIteration.getProject())). //
				flatMap(moduleIteration -> getTicketsFor(moduleIteration, forCurrentUser).stream()). //
				collect(Tickets.toTicketsCollector());

		return tickets;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.tracker.IssueTracker#createReleaseVersion(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void createReleaseVersion(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();
		Optional<Milestone> milestone = findMilestone(moduleIteration, repositoryName);

		if (milestone.isPresent()) {
			return;
		}

		GithubMilestone githubMilestone = new GithubMilestone(moduleIteration);
		logger.log(moduleIteration, "Creating GitHub milestone %s", githubMilestone);

		HttpHeaders httpHeaders = new HttpHeaders();
		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		operations.exchange(MILESTONES_URI_TEMPLATE, HttpMethod.POST,
				new HttpEntity<Object>(githubMilestone.toMilestone(), httpHeaders), Milestone.class, parameters);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.tracker.IssueTracker#retireReleaseVersion(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void archiveReleaseVersion(ModuleIteration module) {
		logger.log(module, "Skipping milestone archival");
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.springframework.data.release.tracker.IssueTracker#createReleaseTicket(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void createReleaseTicket(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");

		Tickets tickets = getTicketsFor(moduleIteration);
		if (tickets.hasReleaseTicket(moduleIteration)) {
			return;
		}

		logger.log(moduleIteration, "Creating release ticket…");

		doCreateTicket(moduleIteration, Tracker.releaseTicketSummary(moduleIteration), TicketType.Task);
	}

	@Override
	public Ticket createTicket(ModuleIteration moduleIteration, String text, TicketType ticketType) {

		logger.log(moduleIteration, "Creating ticket…");

		return doCreateTicket(moduleIteration, text, ticketType);
	}

	private Ticket doCreateTicket(ModuleIteration moduleIteration, String text, TicketType ticketType) {

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();
		Milestone milestone = getMilestone(moduleIteration, repositoryName);

		Label label = TICKET_LABELS.get(ticketType);

		GitHubWriteIssue gitHubIssue = GitHubWriteIssue.of(text, milestone).withLabel(label.getName());

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		GitHubReadIssue body = operations.exchange(ISSUES_URI_TEMPLATE, HttpMethod.POST,
				new HttpEntity<Object>(gitHubIssue), GitHubReadIssue.class, parameters).getBody();

		return toTicket(body);
	}

	@Cacheable("tickets")
	public Tickets getTicketsFor(ModuleIteration iteration) {
		return getTicketsFor(iteration, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#assignTicketToMe(org.springframework.data.release.jira.Ticket)
	 */
	@Override
	public void assignTicketToMe(Ticket ticket) {
		logger.log("Ticket", "Skipping ticket assignment for %s", ticket);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#assignReleaseTicketToMe(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public Ticket assignReleaseTicketToMe(ModuleIteration module) {

		Assert.notNull(module, "ModuleIteration must not be null.");

		Ticket releaseTicketFor = getReleaseTicketFor(module);
		String repositoryName = GitProject.of(module.getProject()).getRepositoryName();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", stripHash(releaseTicketFor));

		GitHubWriteIssue edit = GitHubWriteIssue.assignedTo(properties.getUsername());

		GitHubReadIssue response = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH,
				new HttpEntity<>(edit, new HttpHeaders()), ISSUE_TYPE, parameters).getBody();

		return toTicket(response);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.jira.IssueTracker#startReleaseTicketProgress(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public Ticket startReleaseTicketProgress(ModuleIteration module) {
		return getReleaseTicketFor(module);
	}

	/**
	 * Close the release ticket.
	 *
	 * @param module
	 * @return
	 */
	public Ticket closeReleaseTicket(ModuleIteration module) {

		Assert.notNull(module, "ModuleIteration must not be null.");

		Ticket releaseTicketFor = getReleaseTicketFor(module);
		GitHubReadIssue response = close(module, releaseTicketFor);

		return toTicket(response);
	}

	private GitHubReadIssue close(ModuleIteration module, Ticket ticket) {

		String repositoryName = GitProject.of(module.getProject()).getRepositoryName();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", stripHash(ticket));

		GitHubWriteIssue edit = GitHubWriteIssue.assignedTo(properties.getUsername()).close();

		return operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH,
				new HttpEntity<>(edit, new HttpHeaders()), ISSUE_TYPE, parameters).getBody();
	}

	private String stripHash(Ticket ticket) {
		return ticket.getId().startsWith("#") ? ticket.getId().substring(1) : ticket.getId();
	}

	private Map<String, Object> newUrlTemplateVariables() {

		Map<String, Object> parameters = new HashMap<>();
		return parameters;
	}

	private Optional<Milestone> findMilestone(ModuleIteration moduleIteration, String repositoryName) {

		AtomicReference<Milestone> milestoneRef = new AtomicReference<>();

		for (String state : Arrays.asList("open", "closed")) {

			Map<String, Object> parameters = newUrlTemplateVariables();
			parameters.put("repoName", repositoryName);
			parameters.put("state", state);

			logger.log(moduleIteration, "Looking up milestone…");

			doWithPaging(MILESTONE_URI, HttpMethod.GET, parameters, new HttpEntity<>(new HttpHeaders()),
					MILESTONES_TYPE, milestones -> {

						Optional<Milestone> milestone = milestones.stream(). //
						filter(m -> m.matches(moduleIteration)). //
						findFirst(). //
						map(m -> {
							logger.log(moduleIteration, "Found milestone %s.", m);
							return m;
						});

						if (milestone.isPresent()) {
							milestoneRef.set(milestone.get());
							return false;
						}

						return true;
					});

			if (milestoneRef.get() != null) {
				break;
			}
		}

		return Optional.ofNullable(milestoneRef.get());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.issues.IssueTracker#closeIteration(org.springframework.data.release.model.ModuleIteration)
	 */
	@Override
	public void closeIteration(ModuleIteration module) {

		// for each module

		// - close all tickets
		// -- make sure only one ticket is open
		// -- resolve open ticket
		// -- close tickets

		// - mark version as released

		HttpHeaders httpHeaders = new HttpHeaders();

		GitProject project = GitProject.of(module.getProject());

		findMilestone(module, project.getRepositoryName()) //
				.filter(Milestone::isOpen) //
				.map(Milestone::markReleased) //
				.ifPresent(milestone -> {

					logger.log(module, "Marking milestone %s as released.", milestone);

					Map<String, Object> parameters = newUrlTemplateVariables();
					parameters.put("repoName", project.getRepositoryName());
					parameters.put("id", milestone.getNumber());

					operations.exchange(MILESTONE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH,
							new HttpEntity<Object>(milestone, httpHeaders), Map.class, parameters);
				});

		// - if no next version exists, create

		closeReleaseTicket(module);
	}

	@Override
	public void closeTicket(ModuleIteration module, Ticket ticket) {
		close(module, ticket);
	}

	@Override
	public Tickets findTickets(ModuleIteration moduleIteration, List<TicketReference> ticketReferences) {

		return findGitHubIssues(moduleIteration, ticketReferences).stream().map(GitHub::toTicket)
				.collect(Tickets.toTicketsCollector());
	}

	List<GitHubReadIssue> findGitHubIssues(ModuleIteration moduleIteration, List<TicketReference> ticketReferences) {

		logger.log(moduleIteration, "Looking up GitHub issues from milestone …");

		Map<String, GitHubReadIssue> issues = getIssuesFor(moduleIteration, false, true)
				.collect(Collectors.toMap(GitHubIssue::getId, Function.identity()));

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		logger.log(moduleIteration, "Resolving GitHub issues …");
		Collection<GitHubReadIssue> foundIssues = ExecutionUtils.runAndReturn(executorService,
				Streamable.of(() -> ticketReferences.stream().filter(it -> it.getId().startsWith("#"))),
				ticketReference -> getTicket(issues, repositoryName, ticketReference));

		List<GitHubReadIssue> gitHubIssues = foundIssues.stream().filter(it -> {
			Ticket ticket = toTicket(it);
			return !ticket.isReleaseTicketFor(moduleIteration) && !ticket.isReleaseTicket();
		}).collect(Collectors.toList());

		logger.log(moduleIteration, "Resolved %s tickets.", gitHubIssues.size());

		return gitHubIssues;
	}

	private GitHubReadIssue getTicket(Map<String, GitHubReadIssue> cache, String repositoryName,
			TicketReference reference) {

		if (cache.containsKey(reference.getId())) {
			return cache.get(reference.getId());
		}

		return findTicket(repositoryName, reference.getId());
	}

	private Tickets getTicketsFor(ModuleIteration moduleIteration, boolean forCurrentUser) {

		return getIssuesFor(moduleIteration, forCurrentUser, false).//
				map(GitHub::toTicket).//
				collect(Tickets.toTicketsCollector());
	}

	/**
	 * @param repositoryName
	 * @param ticketId
	 * @return
	 */
	private GitHubReadIssue findTicket(String repositoryName, String ticketId) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", ticketId.startsWith("#") ? ticketId.substring(1) : ticketId);

		try {

			return operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.GET,
					new HttpEntity<>(new HttpHeaders()), ISSUE_TYPE, parameters).getBody();
		} catch (HttpStatusCodeException e) {

			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}

			throw e;
		}
	}

	/**
	 * @param module
	 * @param ticketReferences
	 */
	public void createOrUpdateRelease(ModuleIteration module, List<TicketReference> ticketReferences) {

		logger.log(module, "Preparing GitHub Release …");

		List<GitHubReadIssue> gitHubIssues = findGitHubIssues(module, ticketReferences);

		ArtifactVersion version = ArtifactVersion.of(module);
		DocumentationMetadata documentation = DocumentationMetadata.of(module.getProject(), version);

		ChangelogGenerator generator = new ChangelogGenerator();
		String releaseBody = generator.generate(gitHubIssues, (changelogSection, s) -> s);
		String documentationLinks = getDocumentationLinks(module, documentation);

		createOrUpdateRelease(module, String.format("## :green_book: Links%n%s%n%s%n", documentationLinks, releaseBody));
		logger.log(module, "GitHub Release up to date");
	}

	private String getDocumentationLinks(ModuleIteration module, DocumentationMetadata documentation) {

		String referenceDocUrl = documentation.getReferenceDocUrl(module.getTrain());
		String apiDocUrl = documentation.getApiDocUrl(module.getTrain());

		String reference = String.format("* [%s %s Reference documentation](%s)", module.getProject().getFullName(),
				documentation.getVersion(module.getTrain()), referenceDocUrl);

		String apidoc = String.format("* [%s %s Javadoc](%s)", module.getProject().getFullName(),
				documentation.getVersion(module.getTrain()), apiDocUrl);

		return String.format("%s%n%s%n", reference, apidoc);
	}

	private void createOrUpdateRelease(ModuleIteration module, String body) {

		String repositoryName = GitProject.of(module.getProject()).getRepositoryName();
		Tag tag = VersionTags.empty(module.getProject()).createTag(module);
		logger.log(module, "Looking up GitHub Release …");

		Iteration iteration = module.getTrainIteration().getIteration();
		boolean prerelase = iteration.isPreview();
		GitHubRelease release = findRelease(repositoryName, tag.getName());

		if (release == null) {
			release = new GitHubRelease(null, tag.getName(), tag.getName(), body, false, prerelase);
			logger.log(module, "Creating new Release …");
			createRelease(repositoryName, release);
		} else {
			release = release.withPrerelease(prerelase).withBody(body);
			logger.log(module, "Updating new Release …");
			updateRelease(repositoryName, release);
		}
	}

	private GitHubRelease findRelease(String repositoryName, String tagName) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("tag", tagName);

		try {
			return operations.exchange(RELEASE_BY_TAG_URI_TEMPLATE, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()),
					GitHubRelease.class, parameters).getBody();
		} catch (HttpStatusCodeException e) {

			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}

			throw e;
		}
	}

	private void createRelease(String repositoryName, GitHubRelease release) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		operations.exchange(RELEASE_URI_TEMPLATE, HttpMethod.POST, new HttpEntity<>(release), GitHubRelease.class,
				parameters);
	}

	private void updateRelease(String repositoryName, GitHubRelease release) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", release.getId());

		operations.exchange(RELEASE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH, new HttpEntity<>(release), GitHubRelease.class,
				parameters);
	}

	private Stream<GitHubReadIssue> getIssuesFor(ModuleIteration moduleIteration, boolean forCurrentUser,
			boolean ignoreMissingMilestone) {

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		Optional<Milestone> optionalMilestone = findMilestone(moduleIteration, repositoryName);

		if (ignoreMissingMilestone && !optionalMilestone.isPresent()) {
			return Stream.empty();
		}

		Milestone milestone = optionalMilestone.orElseThrow(() -> noSuchMilestone(moduleIteration));

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());

		if (forCurrentUser) {
			parameters.put("assignee", properties.getUsername());

			return getForIssues(ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE, parameters);
		}

		return getForIssues(ISSUES_BY_MILESTONE_URI_TEMPLATE, parameters);
	}

	private Stream<GitHubReadIssue> getForIssues(String template, Map<String, Object> parameters) {

		List<GitHubReadIssue> issues = new ArrayList<>();
		doWithPaging(template, HttpMethod.GET, parameters, new HttpEntity<>(new HttpHeaders()), ISSUES_TYPE,
				tickets -> {
					issues.addAll(tickets);
					return true;
				});

		return issues.stream();
	}

	private Milestone getMilestone(ModuleIteration moduleIteration, String repositoryName) {

		Optional<Milestone> milestone = findMilestone(moduleIteration, repositoryName);

		return milestone
				.orElseThrow(() -> noSuchMilestone(moduleIteration));
	}

	private IllegalStateException noSuchMilestone(ModuleIteration moduleIteration) {
		return new IllegalStateException(String.format("No milestone for %s found containing %s!", //
				moduleIteration.getProject().getFullName(), //
				new GithubMilestone(moduleIteration)));
	}

	private static Ticket toTicket(GitHubIssue issue) {
		return new Ticket(issue.getId(), issue.getTitle(), issue.getUrl(), new GithubTicketStatus(issue.getState()));
	}

}
