/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author Oliver Gierke
 */
public class ProjectUnitTests {

	@Test
	public void testname() {

		List<Project> projects = new ArrayList<>(Projects.PROJECTS);
		// Collections.reverse(projects);
		// Collections.sort(projects);

		projects.stream().map(Project::getName).forEach(System.out::println);

		System.out.println();

		assertThat(projects.get(0), is(Projects.BUILD));
		assertThat(projects.get(1), is(Projects.COMMONS));
	}

	/**
	 * @see #28
	 */
	@Test
	public void findsProjectByKey() {
		assertThat(Projects.requiredByName("DATACMNS"), is(Projects.COMMONS));
	}

	@Test
	public void returnsCustomFullNameIfSet() {

		assertThat(Projects.BUILD.getFullName(), is("Spring Data Build"));
		assertThat(Projects.CASSANDRA.getFullName(), is("Spring Data for Apache Cassandra"));
	}
}
