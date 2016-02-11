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
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.EqualsAndHashCode;

import org.springframework.data.release.git.Branch;
import org.springframework.util.Assert;

/**
 * Value object to represent a collection of {@link Branch}es with optionally assigned tickets.
 * 
 * @author Mark Paluch
 */
@EqualsAndHashCode
public class TicketBranches implements Iterable<Branch> {

	private Map<Branch, Ticket> ticketBranches;

	/**
	 * Creates a new {@link TicketBranches} instance for the given {@link Map} of {@link Branch}es and {@link Ticket}s.
	 *
	 * @param ticketBranches must not be {@literal null}.
	 */
	TicketBranches(Map<Branch, Ticket> ticketBranches) {

		Assert.notNull(ticketBranches, "TicketBranches must not be null!");
		this.ticketBranches = ticketBranches;
	}

	public static TicketBranches from(Map<Branch, Ticket> ticketBranches) {

		return new TicketBranches(ticketBranches);
	}

	@Override
	public Iterator<Branch> iterator() {
		return ticketBranches.keySet().iterator();
	}

	public Stream<Branch> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	public Optional<Ticket> getTicket(Branch branch) {
		Assert.notNull(branch, "Branch must not be null!");
		return Optional.ofNullable(ticketBranches.get(branch));
	}
}
