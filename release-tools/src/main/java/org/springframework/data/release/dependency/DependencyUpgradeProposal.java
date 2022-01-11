/*
 * Copyright 2020-2022 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor
class DependencyUpgradeProposal {

	DependencyVersion current, latest, latestMinor, proposal;
	List<DependencyVersion> newerVersions;

	public static DependencyUpgradeProposal of(DependencyUpgradePolicy policy, DependencyVersion currentVersion,
			DependencyVersion latestMinor, DependencyVersion latest, List<DependencyVersion> newerVersions) {

		if (policy.restrictToMinorVersion()) {
			return new DependencyUpgradeProposal(currentVersion, latest, latestMinor, latestMinor, newerVersions);
		}

		return new DependencyUpgradeProposal(currentVersion, latest, latestMinor, latest, newerVersions);
	}

	public String getNewVersions(boolean includeAll, boolean includeDate) {

		if (includeAll) {
			return getNewerVersions().stream().map(dependencyVersion -> {

				if (includeDate && dependencyVersion.getCreatedAt() != null) {
					return String.format("%s (%s)", dependencyVersion.getIdentifier(),
							dependencyVersion.getCreatedAt().toLocalDate());
				}

				return dependencyVersion.getIdentifier();
			}).collect(Collectors.joining(", "));
		}

		if (latestMinor.toString().equals(latest.toString())) {
			return latest.toString();
		}

		return String.format("%s, %s", latestMinor, latest);
	}

	public boolean isUpgradeAvailable() {
		return !getCurrent().getIdentifier().equals(getProposal().getIdentifier());
	}

	@Override
	public String toString() {
		return getProposal().getIdentifier();
	}
}
