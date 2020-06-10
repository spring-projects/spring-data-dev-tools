/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.release.issues;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ListWrapperCollector;
import org.springframework.data.util.Streamable;

/**
 * Value object to represent a list of {@link Ticket}s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor
public class Tickets implements Streamable<Ticket> {

	List<Ticket> tickets;
	int overallTotal;

	public Tickets(List<Ticket> tickets) {
		this(Collections.unmodifiableList(tickets), tickets.size());
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
		return findReleaseTicket(moduleIteration).isPresent();
	}

	public Ticket getReleaseTicket(ModuleIteration moduleIteration) {

		return findReleaseTicket(moduleIteration).orElseThrow(
				() -> new IllegalArgumentException(String.format("Did not find a release ticket for %s containing %s!",
						moduleIteration, Tracker.releaseTicketSummary(moduleIteration))));
	}

	public Tickets getIssueTickets(ModuleIteration moduleIteration) {
		return tickets.stream(). //
				filter(ticket -> !ticket.isReleaseTicketFor(moduleIteration)).//
				collect(toTicketsCollector());
	}

	public Tickets getReleaseTickets(TrainIteration iteration) {

		return stream().//
				filter(ticket -> ticket.isReleaseTicketFor(iteration)).//
				distinct().//
				collect(toTicketsCollector());
	}

	private Optional<Ticket> findReleaseTicket(ModuleIteration moduleIteration) {

		return stream().//
				filter(ticket -> ticket.isReleaseTicketFor(moduleIteration)).//
				findFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append(String.format("Train only tickets: %s of %s", tickets.size(), overallTotal));
		builder.append(IOUtils.LINE_SEPARATOR);
		builder.append(tickets.stream().map(it -> "\t" + it).collect(Collectors.joining(IOUtils.LINE_SEPARATOR)));

		return builder.toString();
	}

	/**
	 * Returns a new collector to toTicketsCollector {@link Ticket} as {@link Tickets} using the {@link Stream} API.
	 *
	 * @return
	 */
	public static Collector<? super Ticket, ?, Tickets> toTicketsCollector() {
		return ListWrapperCollector.collectInto(Tickets::new);
	}
}
