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

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Workspace {

	public static final String WORK_DIR_PROPERTY = "io.workDir";

	private final Environment environment;

	public File getWorkingDirectory() {

		String workDir = environment.getProperty("io.workDir");
		return new File(workDir.replace("~", System.getProperty("user.home")));
	}

	public File getProjectDirectory(Project project) {
		return new File(getWorkingDirectory(), project.getName());
	}

	public boolean hasProjectDirectory(Project project) {
		return getProjectDirectory(project).exists();
	}

	public File getFile(String name, Project project) {
		return new File(new File(getWorkingDirectory(), project.getName()), name);
	}

	public boolean exists(String subfolder) {
		return new File(getWorkingDirectory(), subfolder).exists();
	}

	@PostConstruct
	public void setUp() throws IOException {

		Path path = getWorkingDirectory().toPath();

		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}
	}
}
