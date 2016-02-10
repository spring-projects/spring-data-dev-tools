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

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.io.OsOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Slf4j
@Component
class MavenRuntime {

	private final Invoker invoker;
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
	@Autowired
	public MavenRuntime(Workspace workspace, OsOperations os, Logger logger, MavenProperties properties) {

		this.workspace = workspace;
		this.os = os;
		this.logger = logger;
		this.properties = properties;

		this.invoker = new DefaultInvoker();
		this.invoker.setMavenHome(properties.getMavenHome());
		this.invoker.setOutputHandler(line -> log.info(line));
		this.invoker.setErrorHandler(line -> log.info(line));

		File localRepository = properties.getLocalRepository();

		if (localRepository != null) {
			this.invoker.setLocalRepositoryDirectory(localRepository);
		}
	}

	public void execute(Project project, String... arguments) {
		execute(project, Arrays.asList(arguments));
	}

	public void execute(Project project, List<String> arguments) {

		DefaultInvocationRequest request = new DefaultInvocationRequest();
		request.setJavaHome(os.getJavaHome());
		request.setShellEnvironmentInherited(true);
		request.setBaseDirectory(workspace.getProjectDirectory(project));

		List<String> goals = new ArrayList<>();
		goals.add(properties.getFullyQualifiedPlugin(arguments.get(0)));
		goals.addAll(arguments.subList(1, arguments.size()));

		request.setGoals(goals);

		logger.log(project, "Executing mvn %s", goals.stream().collect(Collectors.joining(" ")));

		try {

			InvocationResult result = invoker.execute(request);

			if (result.getExitCode() != 0) {
				throw new RuntimeException(result.getExecutionException());
			}

		} catch (MavenInvocationException o_O) {
			throw new RuntimeException(o_O);
		}
	}
}
