/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.issues.github;

import lombok.Value;

import java.util.Collections;
import java.util.List;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
class GitHubIssue implements Comparable<GitHubIssue> {

	String number, title, state, url;
	GitHubUser user;
	List<Object> assignees;
	String htmlUrl;
	Object milestone;
	PullRequest pullRequest;
	List<Label> labels;

	public GitHubIssue(String number, String title, String state, String url, GitHubUser user, List<Object> assignees,
			@JsonProperty("html_url") String htmlUrl, Object milestone, @JsonProperty("pull_request") PullRequest pullRequest,
			List<Label> labels) {
		this.number = number;
		this.title = title;
		this.state = state;
		this.url = url;
		this.user = user;
		this.assignees = assignees;
		this.htmlUrl = htmlUrl;
		this.milestone = milestone;
		this.pullRequest = pullRequest;
		this.labels = labels;
	}

	public static GitHubIssue of(String title, Milestone milestone) {
		return new GitHubIssue(null, title, null, null, null, null, null, milestone.number, null, null);
	}

	public static GitHubIssue assignedTo(String username) {

		Assert.hasText(username, "Username must not be null or empty!");
		return new GitHubIssue(null, null, null, null, null, Collections.singletonList(username), null, null, null, null);
	}

	public GitHubIssue close() {
		return new GitHubIssue(this.number, this.title, "closed", null, null, this.assignees, null, this.milestone, null,
				null);
	}

	public String getId() {
		return number == null ? null : "#".concat(number);
	}

	@Override
	public int compareTo(GitHubIssue o) {

		int number = this.number != null ? Integer.parseInt(this.number) : -1;
		int other = o.number != null ? Integer.parseInt(o.number) : -1;

		return Integer.compare(number, other);
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

		@JsonIgnore
		public boolean isOpen() {
			return "open".equals(state);
		}

		public Milestone markReleased() {
			return new Milestone(number, null, null, "closed");
		}
	}
}
