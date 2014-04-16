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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.release.jira.Credentials;
import org.springframework.data.release.jira.JiraConnector;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@Component
public class JiraCommands implements CommandMarker {

	private final JiraConnector connector;
	private final Credentials credentials;

	/**
	 * @param connector
	 * @param environment
	 */
	@Autowired
	public JiraCommands(JiraConnector connector, Environment environment) {

		String username = environment.getProperty("jira.username", (String) null);
		String password = environment.getProperty("jira.password", (String) null);

		this.connector = connector;
		this.credentials = StringUtils.hasText(username) ? new Credentials(username, password) : null;
	}

	@CliCommand("jira evict")
	public void jiraEvict() {
		connector.reset();
	}

	@CliCommand(value = "jira tickets")
	public String jira(
			@CliOption(key = { "", "train" }, mandatory = true, help = "The name of the release train.") String trainName, //
			@CliOption(key = "iteration", mandatory = true, help = "An iteration key (one of M1, RC1, GA).") String iterationName, //
			@CliOption(key = "for-current-user", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean forCurrentUser) {

		if (forCurrentUser && credentials == null) {
			return "No authentication specified! Use 'jira authenticate' first!";
		}

		Train train = ReleaseTrains.getTrainByName(trainName);
		Iteration iteration = train.getIterations().getIterationByName(iterationName);

		return connector.getTicketsFor(train, iteration, forCurrentUser ? credentials : null).toString();
	}

	@CliCommand("changelog")
	public String changelog(@CliOption(key = { "", "train" }, mandatory = true) String trainName, //
			@CliOption(key = { "iteration" }, mandatory = true) String iterationName, //
			@CliOption(key = "module") String moduleName) {

		Train train = ReleaseTrains.getTrainByName(trainName);
		Iteration iteration = train.getIteration(iterationName);

		if (StringUtils.hasText(moduleName)) {
			return connector.getChangelogFor(train.getModuleIteration(iteration, moduleName)).toString();
		}

		return "";
	}
}
