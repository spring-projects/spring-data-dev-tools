/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for {@link UpdateInformation}.
 * 
 * @author Oliver Gierke
 */
public class UpdateInformationUnitTests {

	TrainIteration hopperM1 = new TrainIteration(ReleaseTrains.HOPPER, Iteration.M1);

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTrainIteration() {
		new UpdateInformation(null, Phase.CLEANUP);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPhase() {
		new UpdateInformation(hopperM1, null);
	}

	@Test
	public void exposesMilestoneRepositoryForMilestone() {
		assertThat(new UpdateInformation(hopperM1, Phase.PREPARE).getRepository().getId(), is("spring-libs-milestone"));
	}

	@Test
	public void exposesReleaseRepositoryForGA() {

		Arrays.asList(Iteration.GA, Iteration.SR1).forEach(iteration -> {
			TrainIteration trainIteration = new TrainIteration(ReleaseTrains.HOPPER, iteration);
			assertThat(new UpdateInformation(trainIteration, Phase.PREPARE).getRepository().getId(),
					is("spring-libs-release"));
		});
	}

	@Test
	public void calculatesProjectVersionToSetCorrectly() {

		UpdateInformation updateInformation = new UpdateInformation(hopperM1, Phase.PREPARE);
		assertThat(updateInformation.getProjectVersionToSet(Projects.JPA).toString(), is("1.10.0.M1"));

		updateInformation = new UpdateInformation(hopperM1, Phase.CLEANUP);
		assertThat(updateInformation.getProjectVersionToSet(Projects.JPA).toString(), is("1.10.0.BUILD-SNAPSHOT"));
	}
}
