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
import org.springframework.data.release.issues.Changelog;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.TicketReference;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.issues.github.GitHubIssue.Milestone;
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

	private static final ParameterizedTypeReference<List<Milestone>> MILESTONES_TYPE = new ParameterizedTypeReference<List<Milestone>>() {};
	private static final ParameterizedTypeReference<List<GitHubIssue>> ISSUES_TYPE = new ParameterizedTypeReference<List<GitHubIssue>>() {};
	private static final ParameterizedTypeReference<GitHubIssue> ISSUE_TYPE = new ParameterizedTypeReference<GitHubIssue>() {};

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

			GitHubIssue ticket = findTicket(repositoryName, ticketId);
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
				new HttpEntity<Object>(githubMilestone.toMilestone(), httpHeaders), GitHubIssue.Milestone.class, parameters);
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

		doCreateTicket(moduleIteration, Tracker.releaseTicketSummary(moduleIteration));
	}

	@Override
	public Ticket createTicket(ModuleIteration moduleIteration, String text) {

		logger.log(moduleIteration, "Creating ticket…");

		return doCreateTicket(moduleIteration, text);
	}

	private Ticket doCreateTicket(ModuleIteration moduleIteration, String text) {
		HttpHeaders httpHeaders = new HttpHeaders();

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();
		Milestone milestone = getMilestone(moduleIteration, repositoryName);
		GitHubIssue gitHubIssue = GitHubIssue.of(text, milestone);

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);

		GitHubIssue body = operations.exchange(ISSUES_URI_TEMPLATE, HttpMethod.POST,
				new HttpEntity<Object>(gitHubIssue, httpHeaders), GitHubIssue.class, parameters).getBody();

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

		GitHubIssue edit = GitHubIssue.assignedTo(properties.getUsername());

		GitHubIssue response = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH,
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
		String repositoryName = GitProject.of(module.getProject()).getRepositoryName();

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", stripHash(releaseTicketFor));

		GitHubIssue edit = GitHubIssue.assignedTo(properties.getUsername()).close();

		GitHubIssue response = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.PATCH,
				new HttpEntity<>(edit, new HttpHeaders()), ISSUE_TYPE, parameters).getBody();

		return toTicket(response);
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

						Optional<GitHubIssue.Milestone> milestone = milestones.stream(). //
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
	public Tickets resolve(ModuleIteration moduleIteration, List<TicketReference> ticketReferences) {

		logger.log(moduleIteration, "Looking up GitHub issues from milestone …");

		Map<String, GitHubIssue> issues = getIssuesFor(moduleIteration, false, true)
				.collect(Collectors.toMap(GitHubIssue::getId, Function.identity()));

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		logger.log(moduleIteration, "Resolving GitHub issues …");
		Collection<GitHubIssue> foundIssues = ExecutionUtils.runAndReturn(executorService,
				Streamable.of(() -> ticketReferences.stream().filter(it -> it.getId().startsWith("#"))),
				ticketReference -> getTicket(issues, repositoryName, ticketReference));

		Tickets tickets = foundIssues.stream().map(GitHub::toTicket)
				.filter(it -> it.isReleaseTicketFor(moduleIteration) || !it.isReleaseTicket())
				.collect(Tickets.toTicketsCollector());

		logger.log(moduleIteration, "Resolved %s tickets.", tickets.getOverallTotal());

		return tickets;
	}

	private GitHubIssue getTicket(Map<String, GitHubIssue> cache, String repositoryName, TicketReference reference) {

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
	private GitHubIssue findTicket(String repositoryName, String ticketId) {

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", ticketId.startsWith("#") ? ticketId.substring(1) : ticketId);

		try {

			GitHubIssue gitHubIssue = operations.exchange(ISSUE_BY_ID_URI_TEMPLATE, HttpMethod.GET,
					new HttpEntity<>(new HttpHeaders()), ISSUE_TYPE, parameters).getBody();

			return gitHubIssue;

		} catch (HttpStatusCodeException e) {

			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}

			throw e;
		}
	}

	private Stream<GitHubIssue> getIssuesFor(ModuleIteration moduleIteration, boolean forCurrentUser,
			boolean ignoreMissingMilestone) {

		String repositoryName = GitProject.of(moduleIteration.getProject()).getRepositoryName();

		Optional<Milestone> optionalMilestone = findMilestone(moduleIteration, repositoryName);

		if (ignoreMissingMilestone && !optionalMilestone.isPresent()) {
			return Stream.empty();
		}

		GitHubIssue.Milestone milestone = optionalMilestone.orElseThrow(() -> noSuchMilestone(moduleIteration));

		Map<String, Object> parameters = newUrlTemplateVariables();
		parameters.put("repoName", repositoryName);
		parameters.put("id", milestone.getNumber());

		if (forCurrentUser) {
			parameters.put("assignee", properties.getUsername());

			return getForIssues(ISSUES_BY_MILESTONE_AND_ASSIGNEE_URI_TEMPLATE, parameters);
		}

		return getForIssues(ISSUES_BY_MILESTONE_URI_TEMPLATE, parameters);
	}

	private Stream<GitHubIssue> getForIssues(String template, Map<String, Object> parameters) {

		List<GitHubIssue> issues = new ArrayList<>();
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
		return new Ticket(issue.getId(), issue.getTitle(), issue.getHtmlUrl(), new GithubTicketStatus(issue.getState()));
	}
}
