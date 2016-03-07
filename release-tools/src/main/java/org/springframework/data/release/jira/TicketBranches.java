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
package org.springframework.data.release.jira;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.release.Streamable;
import org.springframework.data.release.git.Branch;
import org.springframework.util.Assert;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Value object to represent a collection of {@link Branch}es with assigned tickets.
 * 
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@EqualsAndHashCode
@RequiredArgsConstructor(staticName = "from")
public class TicketBranches implements Streamable<Branch> {

	private final @NonNull Map<Branch, Ticket> ticketBranches;

	/**
	 * Returns whether there's a ticket available for the given {@link Branch}. If {@code requireResolved} is set to
	 * {@literal true} the answer will only be true if the available ticket is marked resolved.
	 * 
	 * @param branch must not be {@literal null}.
	 * @param requireResolved whether the {@link Ticket} we look for is required to be resolved.
	 * @return
	 */
	public boolean hasTicketFor(Branch branch, boolean requireResolved) {

		Assert.notNull(branch, "Branch must not be null!");

		return getTicket(branch).//
				map(ticket -> requireResolved ? ticket.getTicketStatus().isResolved() : true).//
				orElse(false);
	}

	/**
	 * Returns a {@link TicketBranches} containing only the branches for which resolved {@link Ticket}s are found.
	 * 
	 * @return
	 */
	public TicketBranches getResolvedTickets() {

		return new TicketBranches(ticketBranches.entrySet().stream().//
				filter(entry -> entry.getValue().getTicketStatus().isResolved()).//
				collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	/**
	 * Returns the ticket for the given {@link Branch}.
	 * 
	 * @param branch must not be {@literal null}.
	 * @return
	 */
	public Optional<Ticket> getTicket(Branch branch) {

		Assert.notNull(branch, "Branch must not be null!");
		return Optional.ofNullable(ticketBranches.get(branch));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Branch> iterator() {
		return ticketBranches.keySet().iterator();
	}
}
