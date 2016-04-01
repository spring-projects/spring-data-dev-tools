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

import org.springframework.data.release.model.ModuleIteration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
class GitHubIssue {

	private String number;
	private String title;
	private String state;
	private Object milestone;

	@JsonCreator
	public GitHubIssue(@JsonProperty("number") String number, @JsonProperty("title") String title,
			@JsonProperty("state") String state, @JsonProperty("milestone") Object milestone) {
		this.number = number;
		this.title = title;
		this.state = state;
		this.milestone = milestone;
	}

	public GitHubIssue(String title, Milestone milestone) {

		this.title = title;
		this.milestone = milestone.getNumber();
		this.number = null;
		this.state = null;
	}

	public String getId() {

		if (number == null) {
			return null;
		}
		return "#".concat(number);
	}

	/**
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	@Value
	static class Milestone {

		private final String title;
		private final Long number;
		private final String description;

		@JsonCreator
		public Milestone(@JsonProperty("title") String title, @JsonProperty("number") Long number,
				@JsonProperty("description") String description) {
			this.title = title;
			this.number = number;
			this.description = description;
		}

		public Milestone(String title, String description) {
			this.number = null;
			this.title = title;
			this.description = description;
		}

		public boolean matchesIteration(ModuleIteration moduleIteration) {
			return title.contains(moduleIteration.getShortVersionString());
		}
	}
}
