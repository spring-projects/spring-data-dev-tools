/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.release.issues.github;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Collections;
import java.util.List;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor
class GitHubIssue {

	String number, title, state;
	List<Object> assignees;
	Object milestone;

	public static GitHubIssue of(String title, Milestone milestone) {
		return new GitHubIssue(null, title, null, null, milestone.number);
	}

	public static GitHubIssue assignedTo(String username) {

		Assert.hasText(username, "Username must not be null or empty!");
		return new GitHubIssue(null, null, null, Collections.singletonList(username), null);
	}

	public String getId() {
		return number == null ? null : "#".concat(number);
	}

	/**
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	@Value
	static class Milestone {

		Long number;
		String title, description, state;

		public static Milestone of(String title, String description) {
			return new Milestone(null, title, description, null);
		}

		public boolean matches(ModuleIteration moduleIteration) {
			return title.contains(moduleIteration.getShortVersionString());
		}

		public boolean isOpen() {
			return "open".equals(state);
		}

		public Milestone markReleased() {
			return new Milestone(number, null, null, "closed");
		}
	}
}
