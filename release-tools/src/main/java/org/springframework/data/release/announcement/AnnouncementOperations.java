/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.data.release.announcement;

import static org.springframework.data.release.model.Projects.*;

import org.springframework.data.release.build.MavenArtifact;
import org.springframework.data.release.cli.StaticResources;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@Component
public class AnnouncementOperations {

	/**
	 * Returns the project list and links to be included in the release announcement for the given {@link TrainIteration}.
	 * 
	 * @param iteration must not be {@literal null}.
	 * @return
	 */
	public String getProjectBulletpoints(TrainIteration iteration) {

		Assert.notNull(iteration, "Iteration must not be null!");

		StringBuilder builder = new StringBuilder();

		iteration.getModulesExcept(BUILD).forEach(module -> {

			Project project = module.getProject();

			builder.append("* ");
			builder.append(project.getFullName()).append(" ");
			builder.append(module.getShortVersionString());
			builder.append(" - ");

			MavenArtifact artifact = new MavenArtifact(module);

			builder.append(getMarkDownLink("Artifacts", artifact.getRootUrl()));
			builder.append(" - ");

			StaticResources resources = new StaticResources(module);

			builder.append(getMarkDownLink("JavaDocs", resources.getJavaDocUrl())).append(" - ");
			builder.append(getMarkDownLink("Documentation", resources.getDocumentationUrl())).append(" - ");
			builder.append(getMarkDownLink("Changelog", resources.getChangelogUrl()));

			builder.append("\n");
		});

		return builder.toString();
	}

	private static String getMarkDownLink(String name, String url) {
		return String.format("[%s](%s)", name, url);
	}
}
