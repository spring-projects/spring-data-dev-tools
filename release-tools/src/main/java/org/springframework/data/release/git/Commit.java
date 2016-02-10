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
package org.springframework.data.release.git;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.data.release.jira.Ticket;

/**
 * @author Oliver Gierke
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public class Commit {

	private final Ticket ticket;
	private final String summary;
	private final Optional<String> details;

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append(ticket.getId()).append(" - ").append(summary).append("\n");

		details.ifPresent(it -> {
			builder.append("\n");
			builder.append(it).append("\n");
		});

		return builder.toString();
	}
}
