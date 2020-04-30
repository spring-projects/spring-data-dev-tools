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
package org.springframework.data.release.git;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.IterationVersion;
import org.springframework.data.release.model.SimpleIterationVersion;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.Version;

/**
 * @author Oliver Gierke
 */
class BranchUnitTests {

	@Test
	void testname() {

		IterationVersion iterationVersion = new SimpleIterationVersion(Version.of(1, 4), Iteration.RC1);
		assertThat(Branch.from(iterationVersion).toString()).isEqualTo("master");
	}

	@Test
	void createsBugfixBranchForServiceRelease() {

		IterationVersion iterationVersion = new SimpleIterationVersion(Version.of(1, 4), Iteration.SR1);
		assertThat(Branch.from(iterationVersion).toString()).isEqualTo("1.4.x");
	}

	/**
	 * @see #2
	 */
	@Test
	void detectsIssueBranches() {

		Branch branch = Branch.from("issue/DATACMNS-4711");

		assertThat(branch.isIssueBranch(Tracker.JIRA)).isTrue();
		assertThat(branch.isIssueBranch(Tracker.GITHUB)).isFalse();
	}
}
