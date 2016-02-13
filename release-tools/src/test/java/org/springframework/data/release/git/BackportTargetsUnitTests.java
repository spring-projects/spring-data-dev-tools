/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.release.git;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;

/**
 * Unit tests for {@link BackportTargets}.
 * 
 * @author Oliver Gierke
 */
public class BackportTargetsUnitTests {

	Branch goslingBranch = getBranch(ReleaseTrains.GOSLING);
	Branch fowlerBranch = getBranch(ReleaseTrains.FOWLER);

	/**
	 * @see #11
	 */
	@Test
	public void returnsModuleBranchesForTrains() {

		TrainIteration iteration = new TrainIteration(ReleaseTrains.HOPPER, Iteration.M1);
		ModuleIteration module = iteration.getModule(Projects.COMMONS);

		BackportTargets targets = new BackportTargets(module, Arrays.asList(ReleaseTrains.GOSLING, ReleaseTrains.FOWLER));

		assertThat(targets, is(iterableWithSize(2)));
		assertThat(targets, hasItems(goslingBranch, fowlerBranch));
	}

	/**
	 * @see #11
	 */
	@Test
	public void includesMasterBranchForServiceReleaseSource() {

		TrainIteration iteration = new TrainIteration(ReleaseTrains.GOSLING, Iteration.SR2);
		ModuleIteration module = iteration.getModule(Projects.COMMONS);

		BackportTargets targets = new BackportTargets(module, Arrays.asList(ReleaseTrains.FOWLER));

		assertThat(targets, is(iterableWithSize(2)));
		assertThat(targets, hasItems(Branch.MASTER, fowlerBranch));
	}

	private static Branch getBranch(Train train) {
		return Branch.from(train.getModule(Projects.COMMONS));
	}
}
