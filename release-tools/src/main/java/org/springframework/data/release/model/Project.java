/*
 * Copyright 2014 the original author or authors.
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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Wither;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Project implements Comparable<Project> {

	private final @Getter ProjectKey key;
	private final @Getter String name;
	private final @Wither String fullName;
	private final Collection<Project> dependencies;
	private final @Getter Tracker tracker;
	private final @Wither @Getter ArtifactCoordinates additionalArtifacts;
	private final @Wither boolean skipTests;

	Project(String key, String name) {
		this(key, name, null, Tracker.JIRA);
	}

	Project(String key, String name, Tracker tracker) {
		this(key, name, null, tracker);
	}

	private Project(String key, String name, String fullName, Tracker tracker) {
		this(new ProjectKey(key), name, fullName, Collections.emptySet(), tracker, ArtifactCoordinates.SPRING_DATA, false);
	}

	public boolean uses(Tracker tracker) {
		return this.tracker.equals(tracker);
	}

	public String getFullName() {
		return fullName != null ? fullName : "Spring Data ".concat(name);
	}

	public String getFolderName() {
		return "spring-data-".concat(name.toLowerCase());
	}

	public String getDependencyProperty() {
		return "springdata.".concat(name.toLowerCase());
	}

	public void doWithAdditionalArtifacts(Consumer<ArtifactCoordinate> consumer) {
		additionalArtifacts.getCoordinates().forEach(consumer);
	}

	/**
	 * Returns whether the current project depends on the given one.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public boolean dependsOn(Project project) {

		Assert.notNull(project, "Project must not be null!");

		return dependencies.stream().anyMatch(dependency -> dependency.equals(project) || dependency.dependsOn(project));
	}

	public boolean skipTests() {
		return this.skipTests;
	}

	public Project withDependencies(Project... project) {

		return new Project(key, name, fullName, Arrays.asList(project), tracker, additionalArtifacts, skipTests);
	}

	/**
	 * Returns all dependencies of the current project including transitive ones.
	 * 
	 * @return
	 */
	public Set<Project> getDependencies() {

		return dependencies.stream() //
				.flatMap(dependency -> Stream.concat(Stream.of(dependency), dependency.getDependencies().stream())) //
				.collect(Collectors.toSet());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Project that) {
		return Projects.PROJECTS.indexOf(this) - Projects.PROJECTS.indexOf(that);
	}
}
