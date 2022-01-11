/*
 * Copyright 2014-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Iteration}.
 *
 * @author Mark Paluch
 */
class IterationUnitTests {

	@Test
	void shouldCompareMilestoneIterationsCorrectly() {

		assertThat(Iteration.M1).isEqualByComparingTo(Iteration.M1);
		assertThat(Iteration.M1).isLessThan(Iteration.RC1);
		assertThat(Iteration.M1).isLessThan(Iteration.GA);
		assertThat(Iteration.M1).isLessThan(Iteration.SR1);

		assertThat(Iteration.M2).isLessThan(Iteration.M3);
		assertThat(Iteration.M2).isGreaterThan(Iteration.M1);
	}

	@Test
	void shouldCompareReleaseCandidateIterationsCorrectly() {

		assertThat(Iteration.RC1).isGreaterThan(Iteration.M1);
		assertThat(Iteration.RC1).isEqualByComparingTo(Iteration.RC1);
		assertThat(Iteration.RC1).isLessThan(Iteration.GA);
		assertThat(Iteration.RC1).isLessThan(Iteration.SR1);

		assertThat(Iteration.RC2).isLessThan(Iteration.RC3);
		assertThat(Iteration.RC2).isGreaterThan(Iteration.RC1);
	}

	@Test
	void shouldCompareGAIterationsCorrectly() {

		assertThat(Iteration.GA).isGreaterThan(Iteration.M1);
		assertThat(Iteration.GA).isGreaterThan(Iteration.RC1);
		assertThat(Iteration.GA).isEqualByComparingTo(Iteration.GA);
		assertThat(Iteration.GA).isLessThan(Iteration.SR1);
	}

	@Test
	void shouldCompareServiceReleaseIterationsCorrectly() {

		assertThat(Iteration.SR1).isGreaterThan(Iteration.M1);
		assertThat(Iteration.SR1).isGreaterThan(Iteration.RC1);
		assertThat(Iteration.SR1).isGreaterThan(Iteration.GA);
		assertThat(Iteration.SR1).isEqualByComparingTo(Iteration.SR1);

		assertThat(Iteration.SR2).isLessThan(Iteration.SR3);
		assertThat(Iteration.SR2).isGreaterThan(Iteration.SR1);
	}
}
