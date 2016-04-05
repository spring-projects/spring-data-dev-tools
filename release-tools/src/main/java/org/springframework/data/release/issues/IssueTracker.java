/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.data.release.issues;

import java.util.Collection;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.plugin.core.Plugin;

/**
 * Interface for issue tracker operations.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface IssueTracker extends Plugin<Project> {

	/**
	 * Reset internal state (cache, etc).
	 */
	void reset();

	/**
	 * Returns all {@link Tickets} for the given {@link Train} and {@link Iteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @return
	 */
	Tickets getTicketsFor(TrainIteration iteration);

	/**
	 * Returns all {@link Tickets} for the given {@link Train} and {@link Iteration}.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param forCurrentUser
	 * @return
	 */
	Tickets getTicketsFor(TrainIteration iteration, boolean forCurrentUser);

	/**
	 * Returns the {@link Ticket} that tracks modifications in the context of a release.
	 * 
	 * @param module the module to lookup the {@link Ticket} for, must not be {@literal null}.
	 * @return
	 */
	Ticket getReleaseTicketFor(ModuleIteration module);

	/**
	 * Query the issue tracker for multiple {@link Ticket#id ticket Ids}. Tickets that are not found are not returned
	 * within the result.
	 *
	 * @param project must not be {@literal null}.
	 * @param ticketIds collection of {@link Ticket#id ticket Ids}, must not be {@literal null}.
	 * @return
	 */
	Collection<Ticket> findTickets(Project project, Collection<String> ticketIds);

	/**
	 * Creates a release version if release version is missing.
	 * 
	 * @param module must not be {@literal null}.
	 */
	void createReleaseVersion(ModuleIteration module);

	/**
	 * Create release ticket if release ticket is missing.
	 * <p>
	 * TODO: Return created ticket
	 * 
	 * @param module must not be {@literal null}.
	 */
	void createReleaseTicket(ModuleIteration module);

	/**
	 * Assigns the ticket to the current user.
	 *
	 * @param ticket must not be {@literal null}.
	 * @param credentials must not be {@literal null}.
	 */
	void assignTicketToMe(Ticket ticket);

	/**
	 * Assigns the release ticket for the given {@link ModuleIteration} to the current user.
	 * 
	 * @param module must not be {@literal null}.
	 * @return
	 */
	Ticket assignReleaseTicketToMe(ModuleIteration module);

	/**
	 * Returns the {@link Changelog} for the given {@link ModuleIteration}.
	 * 
	 * @param module must not be {@literal null}.
	 * @return
	 */
	Changelog getChangelogFor(ModuleIteration module);
}
