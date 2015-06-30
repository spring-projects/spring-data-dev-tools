/*
 * Copyright 2014 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@ToString
@EqualsAndHashCode
public class Project {

	private final @Getter ProjectKey key;
	private final @Getter String name;
	private final @Getter List<Project> dependencies;
	private final Tracker tracker;
	private final @Getter List<String> additionalArtifacts;

	Project(String key, String name, List<Project> dependencies) {
		this(key, name, Tracker.JIRA, dependencies, Collections.emptyList());
	}

	Project(String key, String name, List<Project> dependencies, List<String> additionalArtifacts) {
		this(key, name, Tracker.JIRA, dependencies, additionalArtifacts);
	}

	Project(String key, String name, Tracker tracker, List<Project> dependencies, List<String> additionalArtifacts) {

		this.key = new ProjectKey(key);
		this.name = name;
		this.dependencies = dependencies;
		this.tracker = tracker;
		this.additionalArtifacts = additionalArtifacts;
	}

	public boolean uses(Tracker tracker) {
		return this.tracker.equals(tracker);
	}

	public String getFullName() {
		return "Spring Data ".concat(name);
	}

	public String getDependencyProperty() {
		return "springdata.".concat(name.toLowerCase());
	}

	/**
	 * Returns whether the current project depends on the given one.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public boolean dependsOn(Project project) {

		Assert.notNull(project, "Project must not be null!");
		return dependencies.contains(project);
	}
}
