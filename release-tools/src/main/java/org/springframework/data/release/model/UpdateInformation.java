package org.springframework.data.release.model;

import static org.springframework.data.release.model.Projects.*;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.release.build.Repository;
import org.springframework.util.Assert;

/**
 * Value object to expose update information for a given {@link TrainIteration} and phase.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class UpdateInformation {

	private final @NonNull @Getter TrainIteration iteration;
	private final @NonNull @Getter Phase phase;

	/**
	 * Returns the {@link ArtifactVersion} to be set for the given {@link Project}.
	 * 
	 * @param dependency must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ArtifactVersion getProjectVersionToSet(Project dependency) {

		Assert.notNull(dependency, "Project must not be null!");

		ArtifactVersion dependencyVersion = iteration.getModuleVersion(dependency);

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

		ArtifactVersion version = iteration.getModuleVersion(BUILD);

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
		return new Repository(iteration.getIteration());
	}

	/**
	 * Returns the version {@link String} to be used to describe the release train.
	 * 
	 * @return will never be {@literal null}.
	 */
	public String getReleaseTrainVersion() {

		switch (phase) {
			case PREPARE:
				return iteration.toVersionString();
			case CLEANUP:
			case MAINTENANCE:
				return iteration.getTrain().getName().concat("-BUILD-SNAPSHOT");
		}

		throw new IllegalStateException("Unexpected phase detected " + phase + " detected!");
	}
}
