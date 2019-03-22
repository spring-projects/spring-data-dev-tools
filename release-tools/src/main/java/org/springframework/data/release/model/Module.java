/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.release.model;

import lombok.Value;

import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@Value
public class Module implements VersionAware, Comparable<Module> {

	private final Project project;
	private final Version version;
	private final Iteration customFirstIteration;

	Module(Project project, String version) {
		this(project, version, null);
	}

	Module(Project project, String version, String customFirstIteration) {

		Assert.notNull(project, "Project must not be null!");

		this.project = project;
		this.version = Version.parse(version);
		this.customFirstIteration = customFirstIteration == null ? null
				: new Iteration(customFirstIteration, Iteration.RC1);
	}

	public boolean hasName(String name) {
		return project.getName().equalsIgnoreCase(name);
	}

	public boolean hasCustomFirstIteration() {
		return customFirstIteration != null;
	}

	public Module next(Transition transition) {

		Version nextVersion = Transition.MAJOR.equals(transition) ? version.nextMajor() : version.nextMinor();
		return new Module(project, nextVersion.toString());
	}

	/**
	 * Returns whether the current {@link Module} refers to the same project as the given one.
	 * 
	 * @param module must not be {@literal null}.
	 * @return
	 */
	public boolean hasSameProjectAs(Module module) {
		return this.project.equals(module.project);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Module that) {
		return this.project.compareTo(that.project);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Spring Data %s %s - %s", project.getName(), version, project.getKey());
	}
}
