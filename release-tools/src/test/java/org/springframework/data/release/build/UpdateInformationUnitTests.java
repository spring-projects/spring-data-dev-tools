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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.TrainIteration;

/**
 * Unit tests for {@link UpdateInformation}.
 * 
 * @author Oliver Gierke
 */
public class UpdateInformationUnitTests {

	TrainIteration hopperM1 = new TrainIteration(ReleaseTrains.HOPPER, Iteration.M1);

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTrainIteration() {
		UpdateInformation.of(null, Phase.CLEANUP);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPhase() {
		UpdateInformation.of(hopperM1, null);
	}

	@Test
	public void exposesMilestoneRepositoryForMilestone() {
		assertThat(UpdateInformation.of(hopperM1, Phase.PREPARE).getRepository().getId(), is("spring-libs-milestone"));
	}

	@Test
	public void exposesReleaseRepositoryForGA() {

		Arrays.asList(Iteration.GA, Iteration.SR1).forEach(iteration -> {
			TrainIteration trainIteration = new TrainIteration(ReleaseTrains.HOPPER, iteration);
			assertThat(UpdateInformation.of(trainIteration, Phase.PREPARE).getRepository().getId(),
					is("spring-libs-release"));
		});
	}

	@Test
	public void calculatesProjectVersionToSetCorrectly() {

		UpdateInformation updateInformation = UpdateInformation.of(hopperM1, Phase.PREPARE);
		assertThat(updateInformation.getProjectVersionToSet(Projects.JPA).toString(), is("1.10.0.M1"));

		updateInformation = UpdateInformation.of(hopperM1, Phase.CLEANUP);
		assertThat(updateInformation.getProjectVersionToSet(Projects.JPA).toString(), is("1.10.0.BUILD-SNAPSHOT"));
	}

	/**
	 * @see #22
	 */
	@Test
	public void returnsCorrectReleaseTrainVersions() {

		TrainIteration hopperGa = new TrainIteration(ReleaseTrains.HOPPER, Iteration.GA);
		TrainIteration hopperSr1 = new TrainIteration(ReleaseTrains.HOPPER, Iteration.SR1);

		assertThat(UpdateInformation.of(hopperGa, Phase.PREPARE).getReleaseTrainVersion(), is("Hopper-RELEASE"));
		assertThat(UpdateInformation.of(hopperM1, Phase.PREPARE).getReleaseTrainVersion(), is("Hopper-M1"));
		assertThat(UpdateInformation.of(hopperSr1, Phase.PREPARE).getReleaseTrainVersion(), is("Hopper-SR1"));
	}
}
