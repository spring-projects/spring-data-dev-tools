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
package org.springframework.data.release.jira;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.release.Streamable;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Value object to represent a list of {@link Ticket}s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor
public class Tickets implements Iterable<Ticket>, Streamable<Ticket> {

	private final List<Ticket> tickets;
	private final int overallTotal;

	public Tickets(List<Ticket> tickets) {
		this(tickets, tickets.size());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Ticket> iterator() {
		return tickets.iterator();
	}

	public boolean hasReleaseTicket(ModuleIteration moduleIteration) {
		return releaseTicketStream(Collections.singleton(Tracker.releaseTicketSummary(moduleIteration))).findFirst()
				.isPresent();
	}

	public Ticket getReleaseTicket(ModuleIteration moduleIteration) {

		Optional<Ticket> releaseTicket = releaseTicketStream(
				Collections.singleton(Tracker.releaseTicketSummary(moduleIteration))).findFirst();

		if (releaseTicket.isPresent()) {
			return releaseTicket.get();
		}

		throw new IllegalStateException(String.format("Did not find a release ticket for %s!", moduleIteration));
	}

	public Tickets getIssueTickets(ModuleIteration moduleIteration) {
		return new Tickets(tickets.stream(). //
				filter(ticket -> !ticket.getSummary().equals(Tracker.releaseTicketSummary(moduleIteration))).//
				collect(Collectors.toList()));
	}

	public Tickets getReleaseTickets(TrainIteration iteration) {

		Set<String> releaseTicketSummary = iteration.stream()
				.map(moduleIteration -> Tracker.releaseTicketSummary(moduleIteration)).collect(Collectors.toSet());

		return new Tickets(releaseTicketStream(releaseTicketSummary).collect(Collectors.toList()));

	}

	private Stream<Ticket> releaseTicketStream(Set<String> releaseTicketSummary) {
		return tickets.stream().filter(ticket -> releaseTicketSummary.contains(ticket.getSummary()));
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Train only tickets: %s of %s", tickets.size(), overallTotal));
		builder.append("\n");
		builder.append(StringUtils.collectionToDelimitedString(tickets, "\n"));

		return builder.toString();
	}

}
