/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.springframework.data.release.model.Projects.*;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.util.Assert;

/**
 * Value object to expose update information for a given {@link TrainIteration} and phase.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(staticName = "of")
public class UpdateInformation {

	private final @NonNull @Getter TrainIteration train;
	private final @NonNull @Getter Phase phase;

	/**
	 * Returns the {@link ArtifactVersion} to be set for the given {@link Project}.
	 * 
	 * @param dependency must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ArtifactVersion getProjectVersionToSet(Project dependency) {

		Assert.notNull(dependency, "Project must not be null!");

		ArtifactVersion dependencyVersion = train.getModuleVersion(dependency);

		switch (phase) {
			case PREPARE:
				return dependencyVersion;
			case CLEANUP:
				return dependencyVersion.getNextDevelopmentVersion();
			case MAINTENANCE:
				return dependencyVersion.getNextBugfixVersion();
		}

		throw new IllegalStateException("Unexpected phase detected " + phase + " detected!");
	}

	/**
	 * Returns the {@link ArtifactVersion} to be set for the parent reference.
	 * 
	 * @return will never be {@literal null}.
	 */
	public ArtifactVersion getParentVersionToSet() {

		ArtifactVersion version = train.getModuleVersion(BUILD);

		switch (phase) {
			case PREPARE:
				return version;
			case CLEANUP:
				return version.getNextDevelopmentVersion();
			case MAINTENANCE:
				return version.getNextBugfixVersion();
		}

		throw new IllegalStateException("Unexpected phase detected " + phase + " detected!");
	}

	/**
	 * Returns the {@link Repository} to use (milestone or release).
	 * 
	 * @return will never be {@literal null}.
	 */
	public Repository getRepository() {
		return new Repository(train.getIteration());
	}

	/**
	 * Returns the version {@link String} to be used to describe the release train.
	 * 
	 * @return will never be {@literal null}.
	 */
	public String getReleaseTrainVersion() {

		String version = train.getTrain().getName();

		switch (phase) {
			case PREPARE:
				Iteration iteration = train.getIteration();
				return String.format("%s-%s", version, iteration.isGAIteration() ? "RELEASE" : iteration.getName());
			case CLEANUP:
			case MAINTENANCE:
				return version.concat("-BUILD-SNAPSHOT");
		}

		throw new IllegalStateException("Unexpected phase detected " + phase + " detected!");
	}
}
