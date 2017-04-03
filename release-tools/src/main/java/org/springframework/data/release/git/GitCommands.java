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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@CliComponent
@RequiredArgsConstructor
class GitCommands extends TimedCommand {

	private final @NonNull GitOperations git;

	@CliCommand("git co-train")
	public void checkout(@CliOption(key = "", mandatory = true) Train train) throws Exception {
		git.checkout(train);
	}

	@CliCommand("git co")
	public void checkout(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {
		git.checkout(iteration);
	}

	@CliCommand("git update")
	public void update(@CliOption(key = { "", "train" }, mandatory = true) String trainName)
			throws Exception, InterruptedException {
		git.update(ReleaseTrains.getTrainByName(trainName));
	}

	@CliCommand("git tags")
	public String tags(@CliOption(key = { "project" }, mandatory = true) String projectName) throws Exception {

		Project project = ReleaseTrains.getProjectByName(projectName);

		return StringUtils.collectionToDelimitedString(git.getTags(project).asList(), "\n");
	}

	/**
	 * Resets all projects contained in the given {@link Train}.
	 *
	 * @param iteration
	 * @throws Exception
	 */
	@CliCommand("git reset")
	public void reset(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {
		git.reset(iteration);
	}

	@CliCommand("git prepare")
	public void prepare(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {
		git.prepare(iteration);
	}

	/**
	 * Pushes all changes of all modules of the given {@link TrainIteration} to the remote server. If {@code tags} is
	 * given, only the tags are pushed.
	 *
	 * @param iteration
	 * @param tags
	 * @throws Exception
	 */
	@CliCommand("git push")
	public void push(//
			@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "tags", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") String tags)
			throws Exception {

		boolean pushTags = Boolean.parseBoolean(tags);

		if (pushTags) {
			git.pushTags(iteration.getTrain());
		} else {
			git.push(iteration);
		}
	}

	@CliCommand("git remove tags")
	public void removeTags(@CliOption(key = "", mandatory = true) TrainIteration iteration) {
		git.removeTags(iteration);
	}

	@CliCommand("git backport changelog")
	public void backportChangelogs(@CliOption(key = "", mandatory = true) TrainIteration iteration, //
			@CliOption(key = "target") String trains) {

		List<Train> targets = trains == null ? Collections.emptyList()
				: Stream.of(trains.split(",")).//
						map(it -> ReleaseTrains.getTrainByName(it)).//
						collect(Collectors.toList());

		git.backportChangelogs(iteration, targets);
	}

	/**
	 * List the branches with their tickets of the git repository.
	 *
	 * @param projectName
	 * @return
	 * @throws Exception
	 */
	@CliCommand("git issuebranches")
	@SuppressWarnings("deprecation")
	public Table issuebranches(@CliOption(key = { "" }, mandatory = true) String projectName,
			@CliOption(key = "resolved", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") Boolean resolved)
			throws Exception {

		Project project = ReleaseTrains.getProjectByName(projectName);
		TicketBranches ticketBranches = git.listTicketBranches(project);

		Table table = new Table();
		table.addHeader(1, new TableHeader("Branch"));
		table.addHeader(2, new TableHeader("Status"));
		table.addHeader(3, new TableHeader("Description"));

		ticketBranches.stream().sorted().//
				filter(branch -> ticketBranches.hasTicketFor(branch, resolved)).//
				forEachOrdered(branch -> {

					Optional<Ticket> ticket = ticketBranches.findTicket(branch);

					table.addRow(branch.toString(), //
							ticket.map(t -> t.getTicketStatus().getLabel()).orElse(""), //
							ticket.map(t -> t.getSummary()).orElse(""));
				});

		return table;
	}
}
