/*
 * Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.TicketReference;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.plugin.core.PluginRegistry;

/**
 * Integration tests for {@link ReleaseOperations}.
 *
 * @author Mark Paluch
 */
@Disabled("Requires changes to application-test.properties to enable remote GitHub/Jira access")
class ReleaseOperationsIntegrationTests extends AbstractIntegrationTests {

	@Autowired PluginRegistry<IssueTracker, Project> trackers;

	@Autowired GitOperations gitOperations;

	@Test
	void shouldResolveJiraTickets() {

		TrainIteration from = ReleaseTrains.OCKHAM.getIteration(Iteration.M1);
		TrainIteration to = ReleaseTrains.OCKHAM.getIteration(Iteration.M2);

		List<TicketReference> ticketReferences = gitOperations.getTicketReferencesBetween(Projects.MONGO_DB, from, to);
		IssueTracker tracker = trackers.getRequiredPluginFor(Projects.MONGO_DB);

		Tickets tickets = tracker.findTickets(to.getModule(Projects.MONGO_DB), ticketReferences);

		assertThat(tickets).hasSize(15);
	}

	@Test
	void shouldResolveGitHubTickets() {

		TrainIteration from = ReleaseTrains.OCKHAM.getIteration(Iteration.M1);
		TrainIteration to = ReleaseTrains.OCKHAM.getIteration(Iteration.M2);

		List<TicketReference> ticketReferences = gitOperations.getTicketReferencesBetween(Projects.R2DBC, from, to);
		IssueTracker tracker = trackers.getRequiredPluginFor(Projects.R2DBC);

		Tickets tickets = tracker.findTickets(to.getModule(Projects.R2DBC), ticketReferences);

		assertThat(tickets).hasSize(22);
	}
}
