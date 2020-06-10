/*
 * Copyright 2016-2020 the original author or authors.
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
package org.springframework.data.release.issues.jira;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Unit tests for {@link Tickets}.
 *
 * @author Mark Paluch
 */
class TicketsUnitTests {

	@Test
	void hasReleaseTicketShouldReturnTrue() {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", null, JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		boolean result = tickets.hasReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Projects.JPA, Iteration.GA));
		assertThat(result).isTrue();
	}

	@Test
	void hasReleaseTickeForTicketWithoutTrainNameShouldReturnFalse() {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA", null, JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		boolean result = tickets.hasReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Projects.JPA, Iteration.GA));
		assertThat(result).isFalse();
	}

	@Test
	void getReleaseTicketReturnsReleaseTicket() {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", null, JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		Ticket releaseTicket = tickets
				.getReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Projects.JPA, Iteration.GA));
		assertThat(releaseTicket).isEqualTo(ticket);
	}

	@Test
	void getReleaseTicketReturnsCalverReleaseTicket() {

		Tickets tickets = new Tickets(
				Collections
						.singletonList(new Ticket("1234", "Release 2.4 GA (2020.0.0)", null, JiraTicketStatus.of(false, "", ""))));

		Ticket releaseTicket = tickets
				.getReleaseTicket(ReleaseTrains.OCKHAM.getModuleIteration(Projects.JPA, Iteration.GA));
		assertThat(releaseTicket).isNotNull();

		tickets = new Tickets(
				Collections
						.singletonList(new Ticket("1234", "Release 2.4 M1 (2020.0.0)", null, JiraTicketStatus.of(false, "", ""))));

		releaseTicket = tickets.getReleaseTicket(ReleaseTrains.OCKHAM.getModuleIteration(Projects.JPA, Iteration.M1));
		assertThat(releaseTicket).isNotNull();

		tickets = new Tickets(
				Collections
						.singletonList(new Ticket("1234", "Release 2.4.1 (2020.0.1)", null, JiraTicketStatus.of(false, "", ""))));

		releaseTicket = tickets.getReleaseTicket(ReleaseTrains.OCKHAM.getModuleIteration(Projects.JPA, Iteration.SR1));
		assertThat(releaseTicket).isNotNull();
	}

	@Test
	void getReleaseTicketThrowsExceptionWithoutAReleaseTicket() {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA", null, JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		assertThatIllegalArgumentException().isThrownBy(
				() -> tickets.getReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Projects.JPA, Iteration.GA)));
	}

	@Test
	void getResolvedReleaseTicket() {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", null, JiraTicketStatus.of(true, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		Ticket releaseTicket = tickets
				.getReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Projects.JPA, Iteration.GA));
		assertThat(releaseTicket).isEqualTo(ticket);
	}

	@Test
	void getReleaseTicketsReturnsReleaseTickets() {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", null, JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		Tickets result = tickets
				.getReleaseTickets(ReleaseTrains.HOPPER.getModuleIteration(Projects.JPA, Iteration.GA).getTrainIteration());
		assertThat(result.getTickets().contains(ticket)).isTrue();
	}
}
