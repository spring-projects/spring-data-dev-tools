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

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;

import lombok.Value;

/**
 * Value object to represent a {@link Ticket}.
 * 
 * @author Oliver Gierke
 */
@Value
public class Ticket {

	private final String id;
	private final String summary;
	private final TicketStatus ticketStatus;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%13s - %s", id, summary);
	}

	public boolean isResolved() {
		return ticketStatus.isResolved();
	}

	public boolean isReleaseTicketFor(ModuleIteration moduleIteration) {
		return summary.equals(Tracker.releaseTicketSummary(moduleIteration));
	}

	public boolean isReleaseTicketFor(TrainIteration iteration) {
		return iteration.stream().filter(this::isReleaseTicketFor).findFirst().isPresent();
	}
}
