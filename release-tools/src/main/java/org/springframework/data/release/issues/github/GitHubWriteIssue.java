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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@With
class GitHubWriteIssue implements GitHubIssue {

	String number, title, state;
	List<Object> assignees;
	Long milestone;
	List<String> labels;

	@Override
	public String getUrl() {
		return null;
	}

	public static GitHubWriteIssue of(String title, Milestone milestone) {
		return new GitHubWriteIssue(null, title, null, null, milestone.getNumber(), null);
	}

	public static GitHubWriteIssue assignedTo(String username) {

		Assert.hasText(username, "Username must not be null or empty!");
		return new GitHubWriteIssue(null, null, null, Collections.singletonList(username), null, null);
	}

	public GitHubWriteIssue close() {
		return new GitHubWriteIssue(this.number, this.title, "closed", this.assignees, null, null);
	}

	public GitHubWriteIssue withLabel(String labelName) {

		List<String> labels = new ArrayList<>(this.labels == null ? Collections.emptyList() : this.labels);
		labels.add(labelName);

		return withLabels(labels);
	}

	public List<String> getAssignees() {
		return assignees == null ? Collections.emptyList()
				: assignees.stream().map(Objects::toString).collect(Collectors.toList());
	}

}
