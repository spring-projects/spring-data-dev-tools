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
package org.springframework.data.release.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ModuleIteration implements IterationVersion {

	private final @Getter Module module;
	private final @Getter TrainIteration trainIteration;

	/**
	 * @return the train
	 */
	public Train getTrain() {
		return trainIteration.getTrain();
	}

	public ProjectKey getProjectKey() {
		return module.getProject().getKey();
	}

	public Project getProject() {
		return module.getProject();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.model.IterationVersion#getVersion()
	 */
	@Override
	public Version getVersion() {
		return module.getVersion();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.model.IterationVersion#getIteration()
	 */
	public Iteration getIteration() {

		return trainIteration.getIteration().isInitialIteration() && this.module.hasCustomFirstIteration()
				? module.getCustomFirstIteration() : this.trainIteration.getIteration();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.model.IterationVersion#isServiceIteration()
	 */
	@Override
	public boolean isServiceIteration() {
		return getIteration().isServiceIteration();
	}

	/**
	 * Returns the {@link String} representation of the logical version of the {@link ModuleIteration}. This will
	 * abbreviate trailing zeros and not include the release train name.
	 * 
	 * @return
	 */
	public String getShortVersionString() {

		StringBuilder builder = new StringBuilder();
		builder.append(ArtifactVersion.of(this).toShortString());

		Iteration iteration = trainIteration.getIteration();

		if (!iteration.isServiceIteration()) {
			builder.append(" ").append(iteration.getName());
		}

		return builder.toString();
	}

	public String getMediumVersionString() {

		StringBuilder builder = new StringBuilder();
		builder.append(ArtifactVersion.of(this).toShortString());

		Iteration iteration = trainIteration.getIteration();

		if (iteration.isServiceIteration()) {
			builder.append(" (").append(trainIteration.toString());
		} else {
			builder.append(" ").append(iteration.getName()).append(" (");
			builder.append(trainIteration.getTrain().getName());
		}

		return builder.append(")").toString();
	}

	/**
	 * Returns the {@link String} representation of the logical version of the {@link ModuleIteration}. This will use the
	 * technical version string and append the train iteration.
	 * 
	 * @return
	 */
	public String getFullVersionString() {

		String result = ArtifactVersion.of(this).toString();
		return result.concat(" (").concat(trainIteration.toString()).concat(")");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s %s", module.getProject().getFullName(), getShortVersionString());
	}
}
