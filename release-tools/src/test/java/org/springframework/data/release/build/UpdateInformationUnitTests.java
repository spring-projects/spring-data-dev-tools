/*
 * Copyright 2015-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.TrainIteration;

/**
 * Unit tests for {@link UpdateInformation}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class UpdateInformationUnitTests {

	TrainIteration hopperM1 = new TrainIteration(ReleaseTrains.HOPPER, Iteration.M1);

	@Test
	void rejectsNullTrainIteration() {
		Assertions.assertThatIllegalArgumentException().isThrownBy(() -> UpdateInformation.of(null, Phase.CLEANUP));
	}

	@Test
	void rejectsNullPhase() {
		Assertions.assertThatIllegalArgumentException().isThrownBy(() -> UpdateInformation.of(hopperM1, null));
	}

	@Test
	void exposesMilestoneRepositoryForMilestone() {
		assertThat(UpdateInformation.of(hopperM1, Phase.PREPARE).getRepository().getId())
				.isEqualTo("spring-libs-milestone");
	}

	@Test
	void exposesReleaseRepositoryForGA() {

		Arrays.asList(Iteration.GA, Iteration.SR1).forEach(iteration -> {
			TrainIteration trainIteration = new TrainIteration(ReleaseTrains.HOPPER, iteration);
			assertThat(UpdateInformation.of(trainIteration, Phase.PREPARE).getRepository().getId())
					.isEqualTo("spring-libs-release");
		});
	}

	@Test
	void calculatesProjectVersionToSetCorrectly() {

		UpdateInformation updateInformation = UpdateInformation.of(hopperM1, Phase.PREPARE);
		assertThat(updateInformation.getProjectVersionToSet(Projects.JPA).toString()).isEqualTo("1.10.0.M1");

		updateInformation = UpdateInformation.of(hopperM1, Phase.CLEANUP);
		assertThat(updateInformation.getProjectVersionToSet(Projects.JPA).toString()).isEqualTo("1.10.0.BUILD-SNAPSHOT");
	}

	@Test // #22
	void returnsCorrectReleaseTrainVersions() {

		TrainIteration hopperGa = new TrainIteration(ReleaseTrains.HOPPER, Iteration.GA);
		TrainIteration hopperSr1 = new TrainIteration(ReleaseTrains.HOPPER, Iteration.SR1);

		assertThat(UpdateInformation.of(hopperGa, Phase.PREPARE).getReleaseTrainVersion()).isEqualTo("Hopper-RELEASE");
		assertThat(UpdateInformation.of(hopperM1, Phase.PREPARE).getReleaseTrainVersion()).isEqualTo("Hopper-M1");
		assertThat(UpdateInformation.of(hopperSr1, Phase.PREPARE).getReleaseTrainVersion()).isEqualTo("Hopper-SR1");
	}
}
