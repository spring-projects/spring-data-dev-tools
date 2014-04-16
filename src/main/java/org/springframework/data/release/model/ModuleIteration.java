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
	private final Iteration iteration;
	private final @Getter Train train;

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
		return module.hasCustomFirstIteration() ? module.getCustomFirstIteration() : this.iteration;
	}

	public String getJiraVersionName() {

		Iteration iteration = module.hasCustomFirstIteration() ? module.getCustomFirstIteration() : this.iteration;

		return String.format("%s %s (%s)", module.getVersion(), iteration.getName(), train.getName());
	}

}
