/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.data.release.issues.jira;

import java.util.Optional;

import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Tickets;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.TrainIteration;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
interface JiraConnector extends IssueTracker {

	/**
	 * Verifies the state of all {@link Tickets} before releasing. In particular: Check whether the release ticket exists,
	 * check whether all other issue tickets are in a resolved state.
	 * 
	 * @param iteration must not be {@literal null}.
	 */
	void verifyBeforeRelease(TrainIteration iteration);

	/**
	 * Lookup a JIRA release version.
	 * 
	 * @param moduleIteration must not be {@literal null}.
	 * @return
	 */
	Optional<JiraReleaseVersion> findJiraReleaseVersion(ModuleIteration moduleIteration);
}
