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

import lombok.AllArgsConstructor;

/**
 * @author Mark Paluch
 */
@AllArgsConstructor
public class JiraTicketStatus implements TicketStatus {

	public static final JiraTicketStatus UNKNOWN = new JiraTicketStatus(false, "unknown", null);

	private final boolean resolved;
	private final String status;
	private final String resolution;

	@Override
	public String getLabel() {
		return resolution == null ? status : status + "/" + resolution;
	}

	@Override
	public boolean isResolved() {
		return resolved;
	}
}
