/*
 * Copyright 2014-2022 the original author or authors.
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

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.TicketReference;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

/**
 * Component to execute GitHub related operations.
 *
 * @author Mark Paluch
 */
@CliComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class GitHubCommands extends TimedCommand {

	@NonNull PluginRegistry<IssueTracker, Project> tracker;
	@NonNull GitHub gitHub;
	@NonNull GitOperations git;
	@NonNull GitHubLabels gitHubLabels;
	@NonNull Executor executor;

	@CliCommand(value = "github update labels")
	public void createOrUpdateLabels(@CliOption(key = "", mandatory = true) Project project) {
		gitHubLabels.createOrUpdateLabels(project);
	}

	@CliCommand(value = "github push")
	public void push(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		git.push(iteration);
		git.pushTags(iteration.getTrain());

		createOrUpdateRelease(iteration);
	}

	@CliCommand(value = "github create release")
	public void createOrUpdateRelease(@CliOption(key = "", mandatory = true) TrainIteration iteration) {

		TrainIteration previousIteration = git.getPreviousIteration(iteration);

		ExecutionUtils.run(executor, iteration, it -> {

			if (it.getProject().getTracker() == Tracker.GITHUB) {

				List<String> ticketReferences = git.getTicketReferencesBetween(it.getProject(), previousIteration, iteration)
						.stream().map(TicketReference::getId).collect(Collectors.toList());
				gitHub.createOrUpdateRelease(it, ticketReferences);
			}
		});
	}

}
