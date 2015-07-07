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
package org.springframework.data.release.git;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import org.springframework.data.release.model.Project;

/**
 * @author Oliver Gierke
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public class GitProject {

	private static final String PROJECT_PREFIX = "spring-data";

	private final Project project;
	private final GitServer server;

	/**
	 * Returns the name of the repository the project is using.
	 * 
	 * @return
	 */
	public String getRepositoryName() {
		return String.format("%s-%s", PROJECT_PREFIX, project.getName().toLowerCase());
	}

	/**
	 * Returns the URI of the {@link Project}'s repository.
	 * 
	 * @return
	 */
	public String getProjectUri() {
		return server.getUri() + getRepositoryName();
	}
}
