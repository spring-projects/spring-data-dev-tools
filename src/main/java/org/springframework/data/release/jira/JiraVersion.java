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
package org.springframework.data.release.jira;

import lombok.Value;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Train;

/**
 * @author Oliver Gierke
 */
@Value
class JiraVersion {

	private final Module module;
	private final Train train;
	private final Iteration iteration;

	public JiraVersion(ModuleIteration moduleIteration) {

		this.module = moduleIteration.getModule();
		this.iteration = moduleIteration.getIteration();
		this.train = moduleIteration.getTrain();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		Iteration iteration = module.hasCustomFirstIteration() ? module.getCustomFirstIteration() : this.iteration;

		if (iteration.isServiceIteration()) {
			return String.format("%s.%s (%s %s)", module.getVersion(), iteration.getBugfixValue(), train.getName(),
					iteration.getName());
		}

		return String.format("%s %s (%s)", module.getVersion(), iteration.getName(), train.getName());
	}
}
