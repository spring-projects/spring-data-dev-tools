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
package org.springframework.data.release.jira;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.plugin.core.Plugin;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
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

	/**
	 * Creates release versions if release versions are missing.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @param credentials must not be {@literal null}.
	 */
	void createReleaseVersions(TrainIteration iteration, Credentials credentials);

	/**
	 * Creates a release version if release version is missing.
	 * 
	 * @param moduleIteration must not be {@literal null}.
	 * @param credentials must not be {@literal null}.
	 */
	void createReleaseVersion(ModuleIteration moduleIteration, Credentials credentials);

	/**
	 * Create release tickets if release tickets are missing.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @param credentials must not be {@literal null}.
	 */
	void createReleaseTickets(TrainIteration iteration, Credentials credentials);

	/**
	 * Create release ticket if release ticket is missing.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @param credentials must not be {@literal null}.
	 */
	void createReleaseTicket(ModuleIteration moduleIteration, Credentials credentials);

	Changelog getChangelogFor(ModuleIteration iteration);
}
