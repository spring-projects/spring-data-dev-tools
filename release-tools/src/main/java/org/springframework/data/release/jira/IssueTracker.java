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

import java.util.Collection;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.plugin.core.Plugin;

/**
 * @author Oliver Gierke
 */
public interface IssueTracker extends Plugin<Project> {

	/**
	 * Reset internal state (cache, ...).
	 */
	void reset();

	/**
	 * Returns the {@link Ticket} that tracks modifications in the context of a release.
	 * 
	 * @param module the module to lookup the {@link Ticket} for.
	 * @return
	 */
	Ticket getReleaseTicketFor(ModuleIteration module);

	/**
	 * Query the issue tracker for multiple {@link Ticket#id ticket Ids}. Tickets that are not found are not returned
	 * within the result.
	 *
	 * @param project
	 * @param ticketIds collection of {@link Ticket#id ticket Ids}
	 * @return
	 */
	Collection<Ticket> findTickets(Project project, Collection<String> ticketIds);

	Changelog getChangelogFor(ModuleIteration iteration);

}
