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

import java.util.Arrays;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Oliver Gierke
 */
@ToString
@EqualsAndHashCode
public class Project {

	private final @Getter ProjectKey key;
	private final @Getter String name;
	private final List<Project> dependencies;
	private final Tracker tracker;

	Project(String key, String name, Project... dependencies) {
		this(key, name, Tracker.JIRA, dependencies);
	}

	Project(String key, String name, Tracker tracker, Project... dependencies) {

		this.key = new ProjectKey(key);
		this.name = name;
		this.dependencies = Arrays.asList(dependencies);
		this.tracker = tracker;
	}

	public boolean uses(Tracker tracker) {
		return this.tracker.equals(tracker);
	}

	public String getFullName() {
		return "Spring Data ".concat(name);
	}
}
