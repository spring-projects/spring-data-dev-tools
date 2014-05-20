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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.io.Workspace.LineCallback;
import org.springframework.data.release.jira.Changelog;
import org.springframework.data.release.jira.IssueTracker;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseOperations {

	private static final Set<String> CHANGELOG_LOCATIONS;

	static {

		Set<String> locations = new HashSet<>();
		locations.add("src/main/resources/changelog.txt"); // for Maven projects
		locations.add("docs/src/info/changelog.txt"); // for Gradle projects

		CHANGELOG_LOCATIONS = Collections.unmodifiableSet(locations);
	}

	private final PluginRegistry<IssueTracker, Project> trackers;
	private final Workspace workspace;
	private final GitOperations git;
	private final Logger logger;

	/**
	 * Creates {@link Changelog} instances for all modules of the given {@link Train} and {@link Iteration}.
	 * 
	 * @param train must not be {@literal null}.
	 * @param iteration must not be {@literal null}.
	 * @throws Exception
	 */
	public void prepareChangelogs(TrainIteration iteration) throws Exception {

		Assert.notNull(iteration, "Iteration must not be null!");

		for (ModuleIteration module : iteration) {

			final Changelog changelog = trackers.getPluginFor(module.getProject()).getChangelogFor(module);

			for (String location : CHANGELOG_LOCATIONS) {

				boolean processed = workspace.processFile(location, module.getProject(), new LineCallback() {

					@Override
					public String doWith(String line, long number) {

						if (line.startsWith("=")) {

							StringBuilder builder = new StringBuilder();
							builder.append(line).append("\n\n");
							builder.append(changelog.toString());

							return builder.toString();
						} else {
							return line;
						}
					}
				});

				if (processed) {

					File file = workspace.getFile(location, module.getProject());
					git.commit(module, "Updated changelog.", null, file);

					logger.log(module.getProject(), "Updated changelog %s.", location);
				}
			}
		}
	}

	public void updateResources(TrainIteration iteration) throws Exception {

		for (final ModuleIteration module : iteration) {

			boolean processed = workspace.processFile("src/main/resources/notice.txt", module.getProject(),
					new LineCallback() {

						@Override
						public String doWith(String line, long number) {

							if (number != 0) {
								return line;
							}

							return module.toString();
						}
					});

			if (processed) {
				logger.log(module, "Updated notice.txt.");
			}
		}
	}
}
