/*
 * Copyright 2014 the original author or authors.
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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Iterator;
import java.util.List;

import org.springframework.data.release.Streamable;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@EqualsAndHashCode
public class TrainIteration implements Streamable<ModuleIteration> {

	private final Train train;
	private final Iteration iteration;

	/**
	 * Create a {@link TrainIteration} for a single module.
	 *
	 * @param moduleIteration
	 * @return
	 */
	public static TrainIteration from(ModuleIteration moduleIteration) {

		Train dependencyDescriptors = ReleaseTrains.getTrainByName(moduleIteration.getTrain().getName());

		if (dependencyDescriptors != null) {
			return new AssociatedTrainIteration(moduleIteration.getTrain(), moduleIteration, dependencyDescriptors);
		}

		return new TrainIteration(moduleIteration.getTrain(), moduleIteration.getIteration());
	}

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

	public ModuleIteration getModule(String name) {
		return train.getModuleIteration(iteration, name);
	}

	public ModuleIteration getModule(Project project) {
		return train.getModuleIteration(iteration, project.getName());
	}

	public List<ModuleIteration> getModulesExcept(Project... exclusions) {
		return train.getModuleIterations(iteration, exclusions);
	}

	public boolean contains(Project project) {
		return train.contains(project);
	}

	public ModuleIteration getPreviousIteration(ModuleIteration module) {

		Iteration previousIteration = train.getIterations().getPreviousIteration(iteration);
		return train.getModuleIteration(previousIteration, module.getProject().getName());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s %s", train.getName(), iteration.getName());
	}

	/**
	 * A release {@link TrainIteration} for a single {@link Module} while associating related dependency versions from a
	 * related {@link Train release train}. Detached {@link TrainIteration} allows to release a single module while
	 * considering specific associated dependency versions.
	 */
	private static class AssociatedTrainIteration extends TrainIteration {

		private final ModuleIteration moduleIteration;
		private final Train dependencyDescriptors;

		AssociatedTrainIteration(Train detached, ModuleIteration moduleIteration, Train dependencyDescriptors) {

			super(detached, moduleIteration.getIteration());

			Assert.isTrue(detached.isDetached(), "Detached Train must have detached flag set");
			Assert.isTrue(!dependencyDescriptors.isDetached(),
					"Dependency descriptors train must not have detached flag set");

			this.moduleIteration = moduleIteration;
			this.dependencyDescriptors = dependencyDescriptors;
		}

		@Override
		public ArtifactVersion getModuleVersion(Project project) {

			if (moduleIteration.getProject().equals(project)) {
				return super.getModuleVersion(project);
			}

			return dependencyDescriptors.getModuleVersion(project, moduleIteration.getIteration());
		}
	}
}
