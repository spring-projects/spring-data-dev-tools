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
package org.springframework.data.release.docs;

import static org.springframework.data.release.model.Projects.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.git.Tag;
import org.springframework.data.release.git.Tags;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.io.Workspace.LineCallback;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocumentationOperations {

	private static final String INDEX_LOCATION = "/src/docbkx/index.xml";

	private final Workspace workspace;
	private final GitOperations git;

	public void updateDockbookIncludes(TrainIteration iteration) throws Exception {

		Tags tags = git.getTags(COMMONS);

		ModuleIteration commons = iteration.getModule(COMMONS);
		ModuleIteration previousIteration = iteration.getPreviousIteration(commons);

		final GitProject gitProject = git.getGitProject(COMMONS);
		final Tag previousTag = tags.createTag(previousIteration);
		final Tag newTag = tags.createTag(commons);

		for (ModuleIteration module : iteration) {

			Project project = module.getProject();

			if (!project.dependsOn(COMMONS)) {
				continue;
			}

			workspace.processFile(INDEX_LOCATION, project, new LineCallback() {

				@Override
				public String doWith(String line, long number) {

					boolean isInclude = line.contains("xi:include");
					boolean containsGitRepo = line.contains(gitProject.getRepositoryName());

					return isInclude && containsGitRepo ? line.replace(previousTag.toString(), newTag.toString()) : line;
				}
			});
		}
	}
}
