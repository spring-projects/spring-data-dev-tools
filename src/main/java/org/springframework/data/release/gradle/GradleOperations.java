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
package org.springframework.data.release.gradle;

import static org.springframework.data.release.model.Projects.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.io.Workspace.LineCallback;
import org.springframework.data.release.maven.Repository;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;

/**
 * Gradle specific operations.
 * 
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GradleOperations {

	private static final String BUILD_GRADLE = "build.gradle";
	private static final String GRADLE_PROPERTIES = "gradle.properties";
	private static final String COMMONS_PROPERTY = "springDataCommonsVersion";

	private final Workspace workspace;
	private final Logger logger;

	/**
	 * Updates all Gradle projects contained in the release.
	 * 
	 * @param iteration
	 * @param phase
	 * @throws Exception
	 */
	public void updateProject(TrainIteration iteration, final Phase phase) throws Exception {

		final Repository repository = new Repository(iteration.getIteration());
		final ArtifactVersion commonsVersion = iteration.getModuleVersion(COMMONS);

		for (ModuleIteration module : iteration.getModulesExcept(BUILD)) {

			final Project project = module.getProject();

			if (!isGradleProject(project)) {
				continue;
			}

			workspace.processFile(GRADLE_PROPERTIES, project, new LineCallback() {

				/*
				 * (non-Javadoc)
				 * @see org.springframework.data.release.io.Workspace.LineCallback#doWith(java.lang.String, long)
				 */
				@Override
				public String doWith(String line, long number) {

					if (!line.contains(COMMONS_PROPERTY)) {
						return line;
					}

					ArtifactVersion version = phase.equals(Phase.PREPARE) ? commonsVersion : commonsVersion
							.getNextDevelopmentVersion();

					logger.log(project, "Setting Spring Data Commons version in %s to %s.", GRADLE_PROPERTIES, version);
					return String.format("%s=%s", COMMONS_PROPERTY, version);
				}
			});

			workspace.processFile(BUILD_GRADLE, project, new LineCallback() {

				/*
				 * (non-Javadoc)
				 * @see org.springframework.data.release.io.Workspace.LineCallback#doWith(java.lang.String, long)
				 */
				@Override
				public String doWith(String line, long number) {

					String snapshotUrl = repository.getSnapshotUrl();
					String releaseUrl = repository.getUrl();
					String message = "Switching to Spring repository %s";

					switch (phase) {
						case CLEANUP:
							logger.log(project, message, snapshotUrl);
							return line.contains(releaseUrl) ? line.replace(releaseUrl, snapshotUrl) : line;
						case PREPARE:
						default:
							logger.log(project, message, releaseUrl);
							return line.contains(snapshotUrl) ? line.replace(snapshotUrl, releaseUrl) : line;
					}
				}
			});
		}
	}

	/**
	 * Returns whether the given project is a Gradle project (checks for the presence of a build.gradle file).
	 * 
	 * @param project
	 * @return
	 */
	private boolean isGradleProject(Project project) {
		return workspace.getFile(BUILD_GRADLE, project).exists();
	}
}
