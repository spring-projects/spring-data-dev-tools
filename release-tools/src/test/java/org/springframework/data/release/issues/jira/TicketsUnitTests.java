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

package org.springframework.data.release.issues.jira;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.util.AssertionErrors.fail;

import java.util.Collections;

import org.junit.Test;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Unit tests for {@link Tickets}.
 * 
 * @author Mark Paluch
 */
public class TicketsUnitTests {

	@Test
	public void hasReleaseTicketShouldReturnTrue() throws Exception {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		boolean result = tickets.hasReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Iteration.GA, "JPA"));
		assertThat(result, is(true));
	}

	@Test
	public void hasReleaseTickeForTicketWithoutTrainNameShouldReturnFalse() throws Exception {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA", JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		boolean result = tickets.hasReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Iteration.GA, "JPA"));
		assertThat(result, is(false));
	}

	@Test
	public void getReleaseTicketReturnsReleaseTicket() throws Exception {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		Ticket releaseTicket = tickets.getReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Iteration.GA, "JPA"));
		assertThat(releaseTicket, is(ticket));
	}

	@Test(expected = IllegalStateException.class)
	public void getReleaseTicketThrowsExceptionWithoutAReleaseTicket() throws Exception {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA", JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		tickets.getReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Iteration.GA, "JPA"));
		fail("Missing IllegalStateException");
	}

	@Test
	public void getResolvedReleaseTicket() throws Exception {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", JiraTicketStatus.of(true, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		Ticket releaseTicket = tickets.getReleaseTicket(ReleaseTrains.HOPPER.getModuleIteration(Iteration.GA, "JPA"));
		assertThat(releaseTicket, is(ticket));
	}

	@Test
	public void getReleaseTicketsReturnsReleaseTickets() throws Exception {

		Ticket ticket = new Ticket("1234", "Release 1.10 GA (Hopper)", JiraTicketStatus.of(false, "", ""));
		Tickets tickets = new Tickets(Collections.singletonList(ticket));

		Tickets result = tickets
				.getReleaseTickets(ReleaseTrains.HOPPER.getModuleIteration(Iteration.GA, "JPA").getTrainIteration());
		assertThat(result.getTickets().contains(ticket), is(true));
	}
}
