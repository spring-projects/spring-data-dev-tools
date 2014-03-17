/*
 * Copyright 2011-2012 the original author or authors.
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
import java.io.StringWriter;

import lombok.RequiredArgsConstructor;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.WriterOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.model.Project;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link OsCommandOperations} interface.
 * 
 * @author Stefan Schmidt
 * @author Oliver Gierke
 * @since 1.2.0
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class IoConfigAwareOsCommandOperations implements OsCommandOperations {

	private final Workspace workspace;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.shell.commands.OsOperations#executeCommand(java.lang.String)
	 */
	public CommandExecution executeCommand(String command) throws IOException {
		return executeCommand(command, (String) null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.io.OsCommandOperations#executeCommand(java.lang.String, org.springframework.data.release.model.Project)
	 */
	@Override
	public CommandExecution executeCommand(String command, Project project) throws IOException {
		return executeCommand(command, workspace.getProjectDirectory(project));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.io.OsCommandOperations#executeCommand(java.lang.String, java.io.File)
	 */
	@Override
	public CommandExecution executeCommand(String command, String subfolder) throws IOException {

		File workingDirectory = workspace.getWorkingDirectory();
		File executionDirectory = subfolder == null ? workingDirectory : new File(workingDirectory, subfolder);

		return executeCommand(command, executionDirectory);
	}

	private CommandExecution executeCommand(String command, File executionDirectory) throws IOException {

		StringWriter writer = new StringWriter();
		WriterOutputStream outputStream = new WriterOutputStream(writer);

		CommandLine commandLine = CommandLine.parse(command);
		DefaultExecuteResultHandler executeResultHandler = new DefaultExecuteResultHandler();

		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(executionDirectory);
		executor.setStreamHandler(new PumpStreamHandler(outputStream, null));
		executor.execute(commandLine, executeResultHandler);

		return new CommandExecution(executeResultHandler, writer);
	}
}
