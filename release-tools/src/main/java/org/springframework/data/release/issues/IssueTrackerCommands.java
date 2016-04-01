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
package org.springframework.data.release.issues;

import static org.springframework.data.release.utils.ExecutionUtils.*;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@CliComponent
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
class IssueTrackerCommands extends TimedCommand {

	@NonNull PluginRegistry<IssueTracker, Project> tracker;

	@CliCommand("tracker evict")
	public void evict() {
		StreamSupport.stream(tracker.spliterator(), false).forEach(IssueTracker::reset);
	}

	@CliCommand(value = "tracker tickets")
	public String jira(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "for-current-user", specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false") boolean forCurrentUser) {

		return tracker.getPlugins().stream().//
				flatMap(it -> it.getTicketsFor(iteration, forCurrentUser).stream()).//
				collect(Tickets.toTicketsCollector()).toString();
	}

	@CliCommand(value = "tracker releasetickets")
	public String releaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		return runAndReturn(iteration, module -> getTrackerFor(module).getReleaseTicketFor(module),
				Tickets.toTicketsCollector()).toString();
	}

	@CliCommand(value = "tracker self-assign releasetickets")
	public String jiraSelfAssignReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		return runAndReturn(iteration, module -> getTrackerFor(module).assignReleaseTicketToMe(module),
				Tickets.toTicketsCollector()).toString();
	}

	@CliCommand(value = "tracker create releaseversions")
	public void jiraCreateReleaseVersions(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		iteration.forEach(this::createReleaseVersion);
	}

	@CliCommand(value = "tracker create releasetickets")
	public String createReleaseTickets(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		iteration.stream().//
				forEach(module -> getTrackerFor(module).createReleaseTicket(module));

		evict();

		return releaseTickets(iteration);
	}

	@CliCommand("tracker changelog")
	public String changelog(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "module") String moduleName) {

		if (StringUtils.hasText(moduleName)) {

			ModuleIteration module = iteration.getModule(moduleName);
			return getTrackerFor(module).getChangelogFor(module).toString();
		}

		return ExecutionUtils.runAndReturn(iteration, this::getChangelog).//
				stream().map(it -> it.toString()).collect(Collectors.joining("\n"));
	}

	private Changelog getChangelog(ModuleIteration module) {
		return getTrackerFor(module).getChangelogFor(module);
	}

	private void createReleaseVersion(ModuleIteration moduleIteration) {
		getTrackerFor(moduleIteration).createReleaseVersion(moduleIteration);
	}

	private IssueTracker getTrackerFor(ModuleIteration moduleIteration) {
		return tracker.getPluginFor(moduleIteration.getProject());
	}
}
