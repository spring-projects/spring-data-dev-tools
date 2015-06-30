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

import lombok.Value;

import java.util.Iterator;

/**
 * @author Oliver Gierke
 */
@Value
public class TrainIteration implements Iterable<ModuleIteration> {

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

	public ModuleIteration getModule(String name) {
		return train.getModuleIteration(iteration, name);
	}

	public ModuleIteration getModule(Project project) {
		return train.getModuleIteration(iteration, project.getName());
	}

	public Iterable<ModuleIteration> getModulesExcept(Project... exclusions) {
		return train.getModuleIterations(iteration, exclusions);
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
}
