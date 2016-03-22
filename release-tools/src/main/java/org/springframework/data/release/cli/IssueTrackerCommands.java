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
package org.springframework.data.release.cli;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.release.CliComponent;
import org.springframework.data.release.jira.Changelog;
import org.springframework.data.release.jira.Credentials;
import org.springframework.data.release.jira.GitHubIssueConnector;
import org.springframework.data.release.jira.IssueTracker;
import org.springframework.data.release.jira.JiraConnector;
import org.springframework.data.release.jira.Tickets;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@CliComponent
public class IssueTrackerCommands implements CommandMarker {

	private final PluginRegistry<IssueTracker, Project> tracker;
	private final JiraConnector jira;
	private final GitHubIssueConnector gitHub;
	private final Credentials credentials;

	/**
	 * @param tracker must not be {@literal null}.
	 * @param jira must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	@Autowired
	public IssueTrackerCommands(PluginRegistry<IssueTracker, Project> tracker, JiraConnector jira, GitHubIssueConnector gitHub,
			Environment environment) {

		String username = environment.getProperty("jira.username", (String) null);
		String password = environment.getProperty("jira.password", (String) null);

		this.tracker = tracker;
		this.jira = jira;
		this.gitHub = gitHub;
		this.credentials = StringUtils.hasText(username) ? new Credentials(username, password) : null;
	}

	@CliCommand("jira evict")
	public void jiraEvict() {
		jira.reset();
	}
	
	@CliCommand("github evict")
	public void githubEvict() {
		gitHub.reset();
	}

	@CliCommand(value = "jira tickets")
	public String jira(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "for-current-user", specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false") boolean forCurrentUser) {

		if (forCurrentUser && credentials == null) {
			return "No authentication specified! Use 'jira authenticate' first!";
		}

		return jira.getTicketsFor(iteration, forCurrentUser ? credentials : null).toString();
	}

	@CliCommand(value = "jira releasetickets")
	public String jiraReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		return jira.getTicketsFor(iteration, null).getReleaseTickets(iteration).toString();
	}
	
	@CliCommand(value = "jira self-assign releasetickets")
	public String jiraSelfAssignReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		Tickets releaseTickets = jira.getTicketsFor(iteration, null).getReleaseTickets(iteration);
		releaseTickets.forEach(ticket -> jira.assignTicketToMe(ticket, credentials));
		return jiraReleaseTickets(iteration);
	}

	@CliCommand(value = "jira create releaseversions")
	public void jiraCreateReleaseVersions(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		jira.createReleaseVersions(iteration, credentials);
	}
	
	@CliCommand(value = "jira create releasetickets")
	public String jiraCreateReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		jira.createReleaseTickets(iteration, credentials);

		jira.reset();
		return jiraReleaseTickets(iteration);
	}
	
	@CliCommand(value = "github tickets")
	public String gitHub(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "for-current-user", specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false") boolean forCurrentUser) {
		return gitHub.getTicketsFor(iteration, forCurrentUser).toString();
	}

	@CliCommand(value = "github releasetickets")
	public String gitHubReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		return gitHub.getTicketsFor(iteration, false).getReleaseTickets(iteration).toString();
	}
	
	@CliCommand(value = "github create releaseversions")
	public void gitHubCreateReleaseVersions(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		gitHub.createReleaseVersions(iteration, credentials);
	}
	
	@CliCommand(value = "github create releasetickets")
	public String gitHubCreateReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		gitHub.createReleaseTickets(iteration, credentials);

		gitHub.reset();
		return gitHubReleaseTickets(iteration);
	}

	@CliCommand("tracker changelog")
	public String changelog(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "module") String moduleName) {

		if (StringUtils.hasText(moduleName)) {

			ModuleIteration module = iteration.getModule(moduleName);
			return tracker.getPluginFor(module.getProject()).getChangelogFor(module).toString();
		}

		return ExecutionUtils.runAndReturn(iteration, this::getChangelog).//
				stream().map(it -> it.toString()).collect(Collectors.joining("\n"));
	}

	private Changelog getChangelog(ModuleIteration module) {
		return tracker.getPluginFor(module.getProject()).getChangelogFor(module);
	}
}
