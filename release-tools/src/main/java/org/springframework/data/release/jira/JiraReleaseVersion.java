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

import lombok.Data;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Value object to bind REST responses to.
 * 
 * @author Mark Paluch
 */
@Data
public class JiraReleaseVersion {

	private String id;
	private String name;
	private String project;
	private String description;

	@JsonCreator
	public JiraReleaseVersion(@JsonProperty("id") String id, @JsonProperty("name") String name,
			@JsonProperty("string") String project, @JsonProperty("description") String description) {
		this.id = id;
		this.name = name;
		this.project = project;
		this.description = description;
	}

	public JiraReleaseVersion(ModuleIteration moduleIteration, JiraVersion jiraVersion) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");
		Assert.notNull(jiraVersion, "JiraVersion must not be null.");

		project = moduleIteration.getProjectKey().getKey();
		name = jiraVersion.toString();
		description = jiraVersion.getDescription();
	}

	public boolean hasSameNameAs(JiraVersion jiraVersion) {
		return getName().equals(jiraVersion.toString());
	}
}
