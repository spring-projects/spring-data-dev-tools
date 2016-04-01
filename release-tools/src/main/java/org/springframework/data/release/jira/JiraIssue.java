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

import java.util.Collections;
import java.util.List;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ProjectKey;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.Train;
import org.springframework.util.Assert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object to bind REST responses to.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Data
class JiraIssue {

	private String key;
	private Fields fields;

	/**
	 * Returns whether the ticket is only fixed in the given {@link Train}. {@literal false} indicates the {@link Ticket}
	 * has been resolved for
	 *
	 * @param train
	 * @return
	 */
	public boolean wasBackportedFrom(Train train) {

		List<FixVersions> fixVersions = fields.getFixVersions();
		return fixVersions != null && wasBackportedFrom(train, fixVersions);
	}

	private boolean wasBackportedFrom(Train train, List<FixVersions> fixVersions) {
		return fixVersions.size() > 1 && isPresent(train, fixVersions);
	}

	private boolean isPresent(Train train, List<FixVersions> fixVersions) {
		return fixVersions.stream().//
				filter(fixVersion -> fixVersion.isTrain(train)).//
				findFirst().//
				isPresent();
	}

	/**
	 * Returns whether the ticket is a release ticket.
	 *
	 * @param moduleIteration
	 * @return
	 */
	public boolean isReleaseTicket(ModuleIteration moduleIteration) {

		List<FixVersions> fixVersions = fields.getFixVersions();
		return fixVersions.size() == 1 && isPresent(moduleIteration.getTrain(), fixVersions)
				&& fields.hasSummary(Tracker.releaseTicketSummary(moduleIteration));
	}

	/**
	 * Creates a new Jira issue value object.
	 * 
	 * @return
	 */
	public static JiraIssue create() {

		JiraIssue jiraIssue = new JiraIssue();
		jiraIssue.setFields(new Fields());
		return jiraIssue;
	}

	/**
	 * Creates a new Jira issue value object with a issue type of {@link IssueType#TASK}.
	 * 
	 * @return
	 */
	public static JiraIssue createTask() {
		JiraIssue jiraIssue = create();
		jiraIssue.getFields().setIssuetype(IssueType.TASK);
		return jiraIssue;
	}

	/**
	 * Assign to the user specified in {@link Credentials}.
	 *
	 * @param credentials must not be {@literal null}.
	 * @return
	 */
	public JiraIssue assignTo(Credentials credentials) {
		return assignTo(credentials.getUsername());
	}

	/**
	 * Assign the ticket to {@code username}.
	 * 
	 * @param username must not be empty and not {@literal null}.
	 * @return
	 */
	public JiraIssue assignTo(String username) {

		Assert.hasText(username, "Username must not be empty!");
		getFields().setAssignee(JiraUser.from(username));
		return this;
	}

	/**
	 * Set the project to {@code projectKey}.
	 * 
	 * @param projectKey must not be {@literal null}.
	 * @return
	 */
	public JiraIssue project(ProjectKey projectKey) {

		getFields().setProject(new Project(projectKey));
		return this;
	}

	/**
	 * Set the issue summary.
	 * 
	 * @param summary must not be empty and not {@literal null}.
	 */
	public JiraIssue summary(String summary) {

		Assert.hasText(summary, "Summary must not be empty!");
		getFields().setSummary(summary);
		return this;
	}

	/**
	 * Set the fix version.
	 * 
	 * @param moduleIteration must not be {@literal null}.
	 * @return
	 */
	public JiraIssue fixVersion(ModuleIteration moduleIteration) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null!");
		getFields().setFixVersions(Collections.singletonList(new JiraVersion(moduleIteration).toFixVersions()));

		return this;
	}

	@Data
	static class Fields {

		Project project;
		IssueType issuetype;
		String summary;
		List<FixVersions> fixVersions;
		List<Component> components;
		Status status;
		Resolution resolution;
		JiraUser assignee;

		public boolean hasSummary(String other) {
			return summary.equals(other);
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Component {

		String id;
		String name;

		public static Component from(String name) {

			Assert.hasText(name, "Name must not be empty!");
			return new Component(null, name);
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class FixVersions {

		String id;
		String name;

		/**
		 * Returns whether this {@link FixVersions} matches the given {@link Train}.
		 *
		 * @param train
		 * @return
		 */
		public boolean isTrain(Train train) {
			return name.contains(train.getName());
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class IssueType {

		public final static IssueType TASK = new IssueType("Task");

		String name;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Status {

		String name;
		StatusCategory statusCategory;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Project {

		String key;

		public Project(ProjectKey projectKey) {
			this.key = projectKey.getKey();
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Resolution {

		String name;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class StatusCategory {

		String key;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class JiraUser {

		String name;
		String displayName;

		static JiraUser from(String username) {
			return new JiraUser(username, null);
		}
	}
}
