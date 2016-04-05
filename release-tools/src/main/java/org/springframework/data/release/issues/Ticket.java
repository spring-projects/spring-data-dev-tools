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

import lombok.Value;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.util.Assert;

/**
 * Value object to represent a {@link Ticket}.
 * 
 * @author Oliver Gierke
 */
@Value
public class Ticket {

	String id, summary;
	TicketStatus ticketStatus;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%14s - %s", id, summary);
	}

	public boolean isResolved() {
		return ticketStatus.isResolved();
	}

	/**
	 * Returns whether the current {@link Ticket} is the release ticket for the given {@link ModuleIteration}.
	 * 
	 * @param module must not be {@literal null}.
	 * @return
	 */
	public boolean isReleaseTicketFor(ModuleIteration module) {

		Assert.notNull(module, "Module must not be null!");
		return summary.equals(Tracker.releaseTicketSummary(module));
	}

	/**
	 * Returns whether the current {@link Ticket} is a release ticket for the given {@link TrainIteration}.
	 * 
	 * @param train must not be {@literal null}.
	 * @return
	 */
	public boolean isReleaseTicketFor(TrainIteration train) {
		return train.stream().anyMatch(this::isReleaseTicketFor);
	}
}
