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
package org.springframework.data.release.misc;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Scanner;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.jira.Changelog;
import org.springframework.data.release.jira.JiraConnector;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.io.Files;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseOperations {

	private final JiraConnector jira;
	private final Workspace workspace;

	/**
	 * Creates {@link Changelog} instances for all modules of the given {@link Train} and {@link Iteration}.
	 * 
	 * @param train must not be {@literal null}.
	 * @param iteration must not be {@literal null}.
	 * @throws Exception
	 */
	public void prepareChangelogs(Train train, Iteration iteration) throws Exception {

		Assert.notNull(train, "Release train must not be null!");
		Assert.notNull(iteration, "Iteration must not be null!");

		for (ModuleIteration module : train.getModuleIterations(iteration, ReleaseTrains.BUILD)) {

			Changelog changelog = jira.getChangelogFor(module);
			File file = workspace.getFile("src/main/resources/changelog.txt", module.getProject());
			StringBuilder builder = new StringBuilder();

			try (Scanner scanner = new Scanner(file)) {

				// Copy headline
				builder.append(scanner.nextLine()).append("\n");
				builder.append(scanner.nextLine()).append("\n");

				// Add new changelog
				builder.append(changelog.toString());

				// Append existing
				while (scanner.hasNextLine()) {
					builder.append(scanner.nextLine()).append("\n");
				}
			}

			Files.write(builder, file, Charset.forName("UTF-8"));
		}
	}
}
