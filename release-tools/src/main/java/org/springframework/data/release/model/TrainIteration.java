/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.model;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Iterator;
import java.util.List;

import org.springframework.data.util.Streamable;

/**
 * @author Oliver Gierke
 */
@Value
@RequiredArgsConstructor
public class TrainIteration implements Streamable<ModuleIteration> {

	private final Train train;
	private final Iteration iteration;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ModuleIteration> iterator() {
		return train.getModuleIterations(iteration).iterator();
	}

	public ArtifactVersion getModuleVersion(Project project) {
		return train.getModuleVersion(project, iteration);
	}

	public ModuleIteration getModule(Project project) {
		return train.getModuleIteration(project, iteration);
	}

	public List<ModuleIteration> getModulesExcept(Project... exclusions) {
		return train.getModuleIterations(iteration, exclusions);
	}

	public boolean contains(Project project) {
		return train.contains(project);
	}

	public ModuleIteration getPreviousIteration(ModuleIteration module) {

		Iteration previousIteration = train.getIterations().getPreviousIteration(iteration);
		return train.getModuleIteration(module.getProject(), previousIteration);
	}

	public String getName() {

		if (getTrain().usesCalver()) {
			return getCalver().toMajorMinorBugfix();
		}

		return getTrain().getName();
	}

	public String getReleaseTrainNameAndVersion() {

		if (getTrain().usesCalver()) {

			if (getIteration().isMilestone() || getIteration().isReleaseCandidate()) {
				return String.format("%s-%s", getCalver().toMajorMinorBugfix(), iteration);
			}

			return getCalver().toString();
		}

		if (iteration.isGAIteration()) {
			return String.format("%s-RELEASE", getTrain().getName());
		}

		return String.format("%s-%s", getTrain().getName(), iteration);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s %s", getName(), iteration.getName());
	}

	public Version getCalver() {

		Version version = getTrain().getCalver();

		if (getIteration().isServiceIteration()) {
			return version.withBugfix(getIteration().getBugfixValue());
		}

		return version;
	}

	public String getNextBugfixName() {

		Version version = getTrain().getCalver();
		int currentBugfixLevel = 0;
		if (getIteration().isServiceIteration()) {
			currentBugfixLevel = getIteration().getBugfixValue();
		}

		return version.withBugfix(currentBugfixLevel + 1).toString();
	}
}
