/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.data.release.jira.IssueTracker;
import org.springframework.data.release.jira.JiraConnector;
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
 */
@CliComponent
public class IssueTrackerCommands implements CommandMarker {

	private final PluginRegistry<IssueTracker, Project> tracker;
	private final JiraConnector jira;
	private final Credentials credentials;

	/**
	 * @param tracker must not be {@literal null}.
	 * @param jira must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	@Autowired
	public IssueTrackerCommands(PluginRegistry<IssueTracker, Project> tracker, JiraConnector jira,
			Environment environment) {

		String username = environment.getProperty("jira.username", (String) null);
		String password = environment.getProperty("jira.password", (String) null);

		this.tracker = tracker;
		this.jira = jira;
		this.credentials = StringUtils.hasText(username) ? new Credentials(username, password) : null;
	}

	@CliCommand("jira evict")
	public void jiraEvict() {
		jira.reset();
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
