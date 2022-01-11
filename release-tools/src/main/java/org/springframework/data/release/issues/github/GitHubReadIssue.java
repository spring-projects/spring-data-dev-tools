/*
 * Copyright 2014-2022 the original author or authors.
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
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Read-representation of a GitHub issue.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
class GitHubReadIssue implements Comparable<GitHubReadIssue>, GitHubIssue {

	String number, title, state, url;
	GitHubUser user;
	List<GitHubUser> assignees;
	Milestone milestone;
	PullRequest pullRequest;
	List<Label> labels;

	public GitHubReadIssue(String number, String title, String state, @JsonProperty("html_url") String url,
			GitHubUser user, List<GitHubUser> assignees, Milestone milestone,
			@JsonProperty("pull_request") PullRequest pullRequest, List<Label> labels) {
		this.number = number;
		this.title = title;
		this.state = state;
		this.url = url;
		this.user = user;
		this.assignees = assignees;
		this.milestone = milestone;
		this.pullRequest = pullRequest;
		this.labels = labels;
	}

	@Override
	public List<String> getAssignees() {
		return assignees == null ? Collections.emptyList()
				: assignees.stream().map(GitHubUser::getName).collect(Collectors.toList());
	}

	@Override
	public int compareTo(GitHubReadIssue o) {

		int number = this.number != null ? Integer.parseInt(this.number) : -1;
		int other = o.number != null ? Integer.parseInt(o.number) : -1;

		return Integer.compare(number, other);
	}

}
