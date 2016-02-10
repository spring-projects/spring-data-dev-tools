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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.WriterOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Implementation of {@link OsOperations} interface.
 * 
 * @author Stefan Schmidt
 * @author Oliver Gierke
 * @since 1.2.0
 */
@Component
class CommonsExecOsCommandOperations implements OsOperations {

	private static final Map<String, String> ENVIRONMENT = new HashMap<>();

	private final Workspace workspace;
	private final Logger logger;
	private final File javaHome;

	@Autowired
	public CommonsExecOsCommandOperations(Workspace workspace, Logger logger, IoProperties properties) throws Exception {

		this.workspace = workspace;
		this.logger = logger;
		this.javaHome = detectJavaHome(properties);

		Assert.isTrue(javaHome.exists(), String.format("Java home %s does not exist!", javaHome.getAbsolutePath()));

		ENVIRONMENT.put("JAVA_HOME", javaHome.getAbsolutePath());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.shell.commands.OsOperations#executeCommand(java.lang.String)
	 */
	@Async
	@Override
	public Future<CommandResult> executeCommand(String command) throws IOException {
		return executeCommand((String) null, command);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.io.OsCommandOperations#executeCommand(java.lang.String, org.springframework.data.release.model.Project)
	 */
	@Async
	@Override
	public Future<CommandResult> executeCommand(String command, Project project) throws IOException {

		logger.log(project, command);

		return executeCommand(command, workspace.getProjectDirectory(project), true);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.io.OsCommandOperations#executeAndListen(org.springframework.data.release.model.Project, java.lang.String)
	 */
	@Override
	public Future<CommandResult> executeWithOutput(String command, Project project) throws IOException {

		logger.log(project, command);

		return executeCommand(command, workspace.getProjectDirectory(project), false);
	}

	private Future<CommandResult> executeCommand(String subfolder, String command) throws IOException {

		File workingDirectory = workspace.getWorkingDirectory();
		File executionDirectory = subfolder == null ? workingDirectory : new File(workingDirectory, subfolder);

		return executeCommand(command, executionDirectory, true);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.io.OsCommandOperations#executeForResult(java.lang.String, org.springframework.data.release.model.Project)
	 */
	@Async
	@Override
	public String executeForResult(String command, Project project) throws Exception {
		return executeCommand(command, workspace.getProjectDirectory(project), true).get().getOutput();
	}

	private Future<CommandResult> executeCommand(String command, File executionDirectory, boolean silent)
			throws IOException {

		StringWriter writer = new StringWriter();
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		try (WriterOutputStream outputStream = new WriterOutputStream(writer)) {

			String outerCommand = "/bin/bash -lc";

			CommandLine outer = CommandLine.parse(outerCommand);
			outer.addArgument(command, false);

			DefaultExecutor executor = new DefaultExecutor();
			executor.setWorkingDirectory(executionDirectory);
			executor.setStreamHandler(new PumpStreamHandler(silent ? outputStream : System.out, null));
			executor.execute(outer, ENVIRONMENT, resultHandler);

			resultHandler.waitFor();

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return new AsyncResult<CommandResult>(
				new CommandResult(resultHandler.getExitValue(), writer.toString(), resultHandler.getException()));
	}

	public File getJavaHome() {
		return javaHome;
	}

	private File detectJavaHome(IoProperties properties) throws Exception {

		File javaHome = properties.getJavaHome();

		if (javaHome != null) {
			return javaHome;
		}

		String javaHomePath = executeCommand("/usr/libexec/java_home -F -v 1.8 -a x86_64 -d64").get().getOutput();

		if (javaHomePath.endsWith("\n")) {
			javaHomePath = javaHomePath.substring(0, javaHomePath.length() - 1);
		}

		return new File(javaHomePath);
	}
}
