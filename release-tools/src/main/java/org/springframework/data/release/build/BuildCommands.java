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
package org.springframework.data.release.build;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.CliComponent;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.utils.Logger;
import org.springframework.shell.core.annotation.CliCommand;

/**
 * @author Oliver Gierke
 */
@CliComponent
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class BuildCommands {

	private final @NonNull BuildOperations build;
	private final @NonNull Workspace workspace;
	private final @NonNull Logger logger;

	/**
	 * Removes all Spring Data artifacts from the local repository.
	 * 
	 * @throws IOException
	 */
	@CliCommand("workspace purge artifacts")
	public void purge() throws IOException {

		logger.log("Workspace", "Cleaning up workspace directory at %s.",
				workspace.getWorkingDirectory().getAbsolutePath());

		workspace.purge(build.getLocalRepository(),
				path -> build.getLocalRepository().relativize(path).startsWith("org/springframework/data"));
	}
}
