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
package org.springframework.data.release.git;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.CliComponent;
import org.springframework.data.release.jira.TicketBranches;
import org.springframework.data.release.misc.CodeWorkflowOperations;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ListTableModel;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@CliComponent
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class GitCommands implements CommandMarker {

	private final GitOperations git;
	private final CodeWorkflowOperations codeWorkflowOperations;

	@CliCommand("git co train")
	public void checkout(@CliOption(key = "", mandatory = true) Train train) throws Exception {
		git.checkout(train);
	}

	@CliCommand("git co")
	public void checkout(@CliOption(key = "", mandatory = true) TrainIteration iteration) throws Exception {
		git.checkout(iteration);
	}

	@CliCommand("git update")
	public void checkout(@CliOption(key = { "", "train" }, mandatory = true) String trainName)
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
			@CliOption(key = "target", mandatory = true) String trains) {

		List<Train> targets = Stream.of(trains.split(",")).map(it -> ReleaseTrains.getTrainByName(it))
				.collect(Collectors.toList());

		git.backportChangelogs(iteration, targets);
	}

	/**
	 * List the branches with their tickets of the git repository.
	 *
	 * @param argument a {@link Project} name or a {@link Train} name.
	 * @return
	 * @throws Exception
	 */
	@CliCommand("git ticketbranches")
	public Table ticketbranches(@CliOption(key = { "" }, mandatory = true) String argument,
			@CliOption(key = "resolved") Boolean resolved) throws Exception {

		boolean requireResolved = Optional.ofNullable(resolved).orElse(false);
		List<TicketBranch> ticketBranches = getTicketBranches(argument).stream().//
				map(branches -> {
					if (requireResolved) {
						return branches.getResolvedTickets();
					}
					return branches;
				}).//
				flatMap(branches -> branches.stream().//
						map(branch -> TicketBranch.of(branch.toString(), branches.getTicket(branch))))
				.collect(Collectors.toList());

		ListTableModel<TicketBranch> tableModel = new ListTableModel<>(ticketBranches, (column, ticketBranch) -> {
			switch (column) {
				case 0:
					return ticketBranch.getBranch();
				case 1:
					return ticketBranch.getStatus();
				case 2:
					return ticketBranch.getSummary();
			}

			throw new UnsupportedOperationException("Column " + column + " not available");
		}).withHeader("Branch", "Status", "Summary");

		Table table = new TableBuilder(tableModel).//
				addOutlineBorder(BorderStyle.fancy_light).//
				addInnerBorder(BorderStyle.fancy_light).//
				addHeaderBorder(BorderStyle.fancy_double).build();
		return table;
	}

	private List<TicketBranches> getTicketBranches(String argument) {

		Project project = ReleaseTrains.getProjectByName(argument);
		if (project != null) {
			return codeWorkflowOperations.ticketBranches(project);
		}

		Train train = ReleaseTrains.getTrainByName(argument);
		if (train != null) {
			return codeWorkflowOperations.ticketBranches(train);
		}

		throw new IllegalArgumentException("ReleaseTrain or Project " + argument + " unknown");
	}
}
