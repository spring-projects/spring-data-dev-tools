/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.release.dependency;

import org.springframework.data.release.model.Iteration;

/**
 * Upgrade policy expressing rules how dependency upgrades should happen.
 *
 * @author Mark Paluch
 */
interface DependencyUpgradePolicy {

	DependencyUpgradePolicy LATEST_STABLE = new DependencyUpgradePolicy() {

		@Override
		public boolean milestoneAllowed() {
			return false;
		}

		@Override
		public boolean restrictToMinorVersion() {
			return false;
		}
	};

	/**
	 * @return {@code true} if the use of pre-release dependency versions is allowed.
	 */
	boolean milestoneAllowed();

	/**
	 * @return {@code true} if upgrades only within the minor version (e.g. bugfixes/patch releases) are allowed.
	 */
	boolean restrictToMinorVersion();

	/**
	 * Create a upgrade policy from a {@link Iteration}.
	 *
	 * @param iteration
	 * @return
	 */
	static DependencyUpgradePolicy from(Iteration iteration) {

		return new DependencyUpgradePolicy() {

			@Override
			public boolean milestoneAllowed() {
				return iteration.isMilestone() || iteration.isReleaseCandidate();
			}

			@Override
			public boolean restrictToMinorVersion() {
				return iteration.isPublic();
			}

		};
	}

}
