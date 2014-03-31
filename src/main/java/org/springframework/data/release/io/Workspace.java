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
package org.springframework.data.release.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.release.model.Project;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Abstraction of the workspace that is used to work with the {@link Project}'s repositories, execute builds, etc.
 * 
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Workspace {

	public static final String WORK_DIR_PROPERTY = "io.workDir";

	private final Environment environment;

	/**
	 * Returns the current working directory.
	 * 
	 * @return
	 */
	public File getWorkingDirectory() {

		String workDir = environment.getProperty("io.workDir");
		return new File(workDir.replace("~", System.getProperty("user.home")));
	}

	/**
	 * Returns the directory for the given {@link Project}.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public File getProjectDirectory(Project project) {

		Assert.notNull(project, "Project must not be null!");
		return new File(getWorkingDirectory(), project.getName());
	}

	/**
	 * Returns whether the project directory for the given project already exists.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public boolean hasProjectDirectory(Project project) {

		Assert.notNull(project, "Project must not be null!");
		return getProjectDirectory(project).exists();
	}

	/**
	 * Returns a file with the given name relative to the working directory for the given {@link Project}.
	 * 
	 * @param name must not be {@literal null} or empty.
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public File getFile(String name, Project project) {

		Assert.hasText(name, "Filename must not be null or empty!");
		Assert.notNull(project, "Project must not be null!");

		return new File(getProjectDirectory(project), name);
	}

	/**
	 * Initializes the working directory and creates the folders if necessary.
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	public void setUp() throws IOException {

		Path path = getWorkingDirectory().toPath();

		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}
	}
}
