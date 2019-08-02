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
package org.springframework.data.release.build;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;

import org.springframework.data.release.io.OsOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Slf4j
@Component
class MavenRuntime {

	private final Workspace workspace;
	private final OsOperations os;
	private final Logger logger;
	private final MavenProperties properties;

	/**
	 * Creates a new {@link MavenRuntime} for the given {@link Workspace} and Maven home.
	 *
	 * @param workspace must not be {@literal null}.
	 * @param os must not be {@literal null}.
	 * @param logger must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 */
	public MavenRuntime(Workspace workspace, OsOperations os, Logger logger, MavenProperties properties) {

		this.workspace = workspace;
		this.os = os;
		this.logger = logger;
		this.properties = properties;
	}

	public void execute(Project project, CommandLine arguments) {

		logger.log(project, "Executing mvn %s", arguments.toString());

		try (MavenLogger mavenLogger = getLogger(project, arguments.getGoals())) {

			Invoker invoker = new DefaultInvoker();
			invoker.setMavenHome(properties.getMavenHome());
			invoker.setOutputHandler(mavenLogger::info);
			invoker.setErrorHandler(mavenLogger::warn);

			File localRepository = properties.getLocalRepository();

			if (localRepository != null) {
				invoker.setLocalRepositoryDirectory(localRepository);
			}

			DefaultInvocationRequest request = new DefaultInvocationRequest();
			request.setJavaHome(os.getJavaHome());
			request.setShellEnvironmentInherited(true);
			request.setBaseDirectory(workspace.getProjectDirectory(project));

			request.setGoals(arguments.toCommandLine(it -> properties.getFullyQualifiedPlugin(it.getGoal())));

			InvocationResult result = invoker.execute(request);

			if (result.getExitCode() != 0) {
				logger.warn(project, "Failed execution mvn %s", arguments.toString());

				throw new IllegalStateException("Failed execution mvn " + arguments.toString(), result.getExecutionException());
			}
			logger.log(project, "Successful execution mvn %s", arguments.toString());
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}

	private MavenLogger getLogger(Project project, List<CommandLine.Goal> goals) {

		if (this.properties.isConsoleLogger()) {
			return new SlfLogger(log, project);
		}

		return new FileLogger(log, project, this.workspace.getLogsDirectory(), goals);
	}

	/**
	 * Maven Logging Forwarder.
	 */
	interface MavenLogger extends Closeable {

		void info(String message);

		void warn(String message);
	}

	@RequiredArgsConstructor
	static class SlfLogger implements MavenLogger {

		private final org.slf4j.Logger logger;
		private final String logPrefix;

		SlfLogger(org.slf4j.Logger logger, Project project) {
			this.logger = logger;
			this.logPrefix = StringUtils.padRight(project.getName(), 10);
		}

		@Override
		public void info(String message) {
			logger.info(logPrefix + ": " + message);
		}

		@Override
		public void warn(String message) {
			logger.warn(logPrefix + ": " + message);
		}

		@Override
		public void close() throws IOException {
			// no-op
		}
	}

	static class FileLogger implements MavenLogger {

		private final PrintWriter printWriter;
		private final FileOutputStream outputStream;

		FileLogger(org.slf4j.Logger logger, Project project, File logsDirectory, List<CommandLine.Goal> goals) {

			if (!logsDirectory.exists()) {
				logsDirectory.mkdirs();
			}

			String goalNames = goals.stream().map(CommandLine.Goal::getGoal).collect(Collectors.joining("-"));

			String filename = String.format("mvn-%s-%s.log", project.getName(), goalNames).replace(':', '.');

			try {
				File file = new File(logsDirectory, filename);
				logger.info("Routing Maven output to " + file.getCanonicalPath());
				outputStream = new FileOutputStream(file, true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			printWriter = new PrintWriter(outputStream, true);
		}

		@Override
		public void info(String message) {
			printWriter.println(message);
		}

		@Override
		public void warn(String message) {
			printWriter.println(message);
		}

		@Override
		public void close() throws IOException {
			printWriter.close();
			outputStream.close();
		}
	}

}
