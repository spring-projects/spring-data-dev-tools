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
package org.springframework.data.release.issues.jira;

import lombok.Data;
import lombok.Value;

import java.util.Collections;
import java.util.List;

import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ProjectKey;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.Train;
import org.springframework.util.Assert;

/**
 * Value object to bind REST responses to.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
class JiraIssue {

	String key;
	Fields fields;

	/**
	 * Creates a new Jira issue value object.
	 *
	 * @return
	 */
	public static JiraIssue create() {
		return new JiraIssue(null, new Fields());
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
	 * Returns whether the ticket is only fixed in the given {@link Train}. {@literal false} indicates the {@link Ticket}
	 * has been resolved for
	 *
	 * @param train
	 * @return
	 */
	public boolean wasBackportedFrom(Train train) {

		List<FixVersion> fixVersions = fields.getFixVersions();
		return fixVersions != null && wasBackportedFrom(train, fixVersions);
	}

	/**
	 * Returns whether the ticket is a release ticket.
	 *
	 * @param moduleIteration
	 * @return
	 */
	public boolean isReleaseTicket(ModuleIteration moduleIteration) {

		List<FixVersion> fixVersions = fields.getFixVersions();
		return fixVersions.size() == 1 && isPresent(moduleIteration.getTrain(), fixVersions)
				&& fields.hasSummary(Tracker.releaseTicketSummary(moduleIteration));
	}

	/**
	 * Set the project to {@code projectKey}.
	 *
	 * @param projectKey must not be {@literal null}.
	 * @return
	 */
	public JiraIssue project(ProjectKey projectKey) {

		getFields().setProject(Project.from(projectKey));
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
		getFields().setFixVersions(Collections.singletonList(new JiraVersion(moduleIteration).toFixVersion()));

		return this;
	}

	public boolean isAssignedTo(String username) {
		return getFields().getAssignee() != null && getFields().getAssignee().matches(username);
	}

	private static boolean wasBackportedFrom(Train train, List<FixVersion> fixVersions) {
		return fixVersions.size() > 1 && isPresent(train, fixVersions);
	}

	private static boolean isPresent(Train train, List<FixVersion> fixVersions) {
		return fixVersions.stream().//
				filter(fixVersion -> fixVersion.isTrain(train)).//
				findFirst().//
				isPresent();
	}

	@Data
	static class Fields {

		Project project;
		IssueType issuetype;
		String summary;
		List<FixVersion> fixVersions;
		List<Component> components;
		Status status;
		Resolution resolution;
		JiraUser assignee;

		public boolean hasSummary(String other) {
			return summary.equals(other);
		}

		public boolean hasStatusAndResolution() {
			return status != null && resolution != null;
		}
	}

	@Value
	static class Component {

		String id, name;

		public static Component of(String name) {

			Assert.hasText(name, "Name must not be empty!");
			return new Component(null, name);
		}
	}

	@Value
	static class FixVersion {

		String id, name;

		/**
		 * Returns whether this {@link FixVersion} matches the given {@link Train}.
		 *
		 * @param train
		 * @return
		 */
		public boolean isTrain(Train train) {
			return name.contains(train.getName());
		}
	}

	@Value
	static class IssueType {

		public final static IssueType TASK = new IssueType("Task");

		String name;
	}

	@Value
	static class Status {

		String name;
		StatusCategory statusCategory;
	}

	@Value
	static class Project {

		String key;

		public static Project from(ProjectKey projectKey) {
			return new Project(projectKey.getKey());
		}
	}

	@Value
	static class Resolution {

		String name;
	}

	@Value
	static class StatusCategory {

		String key;
	}

	@Value
	static class JiraUser {

		String name, displayName;

		static JiraUser from(String username) {
			return new JiraUser(username, null);
		}

		public boolean matches(String username) {
			return username.equalsIgnoreCase(name);
		}
	}
}
