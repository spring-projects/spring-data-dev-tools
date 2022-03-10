/*
 * Copyright 2022 the original author or authors.
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;

/**
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TicketOperations {

	Logger logger;

	PluginRegistry<IssueTracker, Project> tracker;

	/**
	 * Create or look up ticket with a particular summary.
	 *
	 * @param module
	 * @param summary
	 * @return
	 */
	public Ticket getOrCreateTicketsWithSummary(ModuleIteration module, IssueTracker.TicketType ticketType,
			String summary) {
		return getOrCreateTicketsWithSummary(module, ticketType, Collections.singletonList(summary)).getTickets().get(0);
	}

	/**
	 * Create or look up tickets with a particular summary.
	 *
	 * @param module
	 * @param ticketType
	 * @param summary
	 * @return
	 */
	public Tickets getOrCreateTicketsWithSummary(ModuleIteration module, IssueTracker.TicketType ticketType,
			List<String> summary) {

		Project project = module.getProject();

		IssueTracker tracker = this.tracker.getRequiredPluginFor(project);
		Tickets tickets = tracker.getTicketsFor(module);
		List<Ticket> results = new ArrayList<>();

		for (String s : summary) {

			Optional<Ticket> upgradeTicket = findBySummary(tickets, s);

			if (upgradeTicket.isPresent()) {
				logger.log(project, "Found ticket %s", upgradeTicket.get());
				upgradeTicket.ifPresent(it -> {
					tracker.assignTicketToMe(project, it);
					results.add(it);
				});
			} else {

				logger.log(module, "Creating ticket for %s", summary);
				Ticket ticket = tracker.createTicket(module, s, ticketType, true);
				results.add(ticket);
			}
		}

		return new Tickets(results);
	}

	private Optional<Ticket> findBySummary(Tickets tickets, String summary) {

		List<Ticket> result = tickets.filter(it -> it.getSummary().equals(summary)).toList();

		if (result.size() > 1) {
			throw new IllegalStateException("Multiple tickets found: " + result);
		}

		return Optional.ofNullable(result.isEmpty() ? null : result.get(0));
	}

	public void closeTicket(ModuleIteration module, Ticket ticket) {
		closeTickets(module, new Tickets(Collections.singletonList(ticket)));
	}

	public void closeTickets(ModuleIteration module, Tickets tickets) {

		IssueTracker tracker = this.tracker.getRequiredPluginFor(module.getProject());

		for (Ticket ticket : tickets) {
			tracker.closeTicket(module, ticket);
		}
	}

}
