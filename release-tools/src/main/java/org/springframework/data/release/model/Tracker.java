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
package org.springframework.data.release.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum Tracker {

	JIRA("(([A-Z]{1,10})+-\\d+)"), //
	GITHUB("((#)?\\d+)");

	private final String ticketPattern;

	public static final String releaseTicketSummary(ModuleIteration moduleIteration) {
		return "Release " + moduleIteration.getMediumVersionString();
	}
}
