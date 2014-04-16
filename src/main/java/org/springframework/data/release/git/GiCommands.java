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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Project;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GiCommands implements CommandMarker {

	private final GitOperations git;

	@CliCommand("git checkout")
	public void checkout(@CliOption(key = { "", "train" }, mandatory = true) String trainName, @CliOption(
			key = "iteration", mandatory = true) String iterationName) throws Exception {

		Train train = ReleaseTrains.getTrainByName(trainName);
		Iteration iteration = train.getIteration(iterationName);

		git.checkout(train, iteration);
	}

	@CliCommand("git update")
	public void checkout(@CliOption(key = { "", "train" }, mandatory = true) String trainName) throws Exception,
			InterruptedException {

		git.update(ReleaseTrains.getTrainByName(trainName));
	}

	@CliCommand("git tags")
	public String tags(@CliOption(key = { "project" }, mandatory = true) String projectName) throws Exception {

		Project project = ReleaseTrains.getProjectByName(projectName);

		return StringUtils.collectionToDelimitedString(git.getTags(project).asList(), "\n");
	}

	@CliCommand("git reset")
	public void reset(@CliOption(key = { "", "train" }, mandatory = true) String trainName, @CliOption(key = "iteration",
			mandatory = true) String iterationName) throws Exception {
		git.reset(ReleaseTrains.getTrainByName(trainName));
	}

	@CliCommand("git prepare")
	public void prepare(@CliOption(key = { "", "train" }, mandatory = true) String trainName, @CliOption(
			key = "iteration", mandatory = true) String iterationName) throws Exception {

		Train train = ReleaseTrains.getTrainByName(trainName);
		git.prepare(train, train.getIteration(iterationName));
	}
}
