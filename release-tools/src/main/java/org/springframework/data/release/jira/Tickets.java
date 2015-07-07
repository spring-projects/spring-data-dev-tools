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

import java.util.Iterator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.springframework.util.StringUtils;

/**
 * Value object to represent a list of {@link Ticket}s.
 * 
 * @author Oliver Gierke
 */
@Value
@RequiredArgsConstructor
public class Tickets implements Iterable<Ticket> {

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

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Train only tickets: %s of %s", tickets.size(), overallTotal));
		builder.append("\n");
		builder.append(StringUtils.collectionToDelimitedString(tickets, "\n"));

		return builder.toString();
	}
}
