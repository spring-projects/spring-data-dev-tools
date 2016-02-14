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
package org.springframework.data.release.git;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.IterationVersion;
import org.springframework.data.release.model.SimpleIterationVersion;
import org.springframework.data.release.model.Tracker;
import org.springframework.data.release.model.Version;

/**
 * @author Oliver Gierke
 */
public class BranchUnitTests {

	@Test
	public void testname() {

		IterationVersion iterationVersion = new SimpleIterationVersion(Version.of(1, 4), Iteration.RC1);
		assertThat(Branch.from(iterationVersion).toString(), is("master"));
	}

	@Test
	public void createsBugfixBranchForServiceRelease() {

		IterationVersion iterationVersion = new SimpleIterationVersion(Version.of(1, 4), Iteration.SR1);
		assertThat(Branch.from(iterationVersion).toString(), is("1.4.x"));
	}

	/**
	 * @see #2
	 */
	@Test
	public void detectsIssueBranches() {

		Branch branch = Branch.from("issue/DATACMNS-4711");

		assertThat(branch.isIssueBranch(Tracker.JIRA), is(true));
		assertThat(branch.isIssueBranch(Tracker.GITHUB), is(false));
	}
}
