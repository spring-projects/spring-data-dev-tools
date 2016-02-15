/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.data.release.misc;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.Streamable;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.jira.TicketBranches;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Component to execute operations related to the code workflow.
 *
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class CodeWorkflowOperations {

	private final GitOperations git;

	/**
	 * Retrieve {@link TicketBranches ticket branches} for a {@link Project}.
	 *
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public List<TicketBranches> ticketBranches(Project project) {

		Assert.notNull(project, "Project must not be null!");
		return ticketBranches(() -> Stream.of(project).iterator());
	}

	/**
	 * Retrieve {@link TicketBranches ticket branches} for a {@link Train release train}.
	 *
	 * @param releaseTrain must not be {@literal null}.
	 * @return
	 */
	public List<TicketBranches> ticketBranches(Train releaseTrain) {

		Assert.notNull(releaseTrain, "Train must not be null!");
		return ticketBranches(() -> releaseTrain.stream().map(module -> module.getProject()).iterator());
	}

	private List<TicketBranches> ticketBranches(Streamable<Project> projectStream) {

		List<TicketBranches> result = new ArrayList<>();

		ExecutionUtils.run(projectStream, project -> {
			TicketBranches ticketBranches = git.listTicketBranches(project);
			synchronized (result) {
				result.add(ticketBranches);
			}
		});

		return result;
	}

}
