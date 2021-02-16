/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.dependency;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.model.Iteration;

/**
 * Unit tests for {@link DependencyOperations}.
 *
 * @author Mark Paluch
 */
class DependencyOperationsUnitTests {

	@Test
	void shouldRetainCurrentVersion() {

		List<DependencyVersion> availableVersions = Stream.of("5.7.0", "5.7.0-M1") //
				.map(DependencyVersion::of) //
				.sorted() //
				.collect(Collectors.toList());

		DependencyUpgradeProposal proposal = DependencyOperations.getDependencyUpgradeProposal(Iteration.SR1,
				DependencyVersion.of("5.7.0"), availableVersions);

		assertThat(proposal.getCurrent()).isEqualTo(DependencyVersion.of("5.7.0"));
		assertThat(proposal.getLatestMinor()).isEqualTo(DependencyVersion.of("5.7.0"));
		assertThat(proposal.getProposal()).isEqualTo(DependencyVersion.of("5.7.0"));
		assertThat(proposal.getLatest()).isEqualTo(DependencyVersion.of("5.7.0"));
	}

	@Test
	void shouldSelectNextMinorVersion() {

		List<DependencyVersion> availableVersions = Stream.of("5.7.0", "5.7.1", "5.8.0") //
				.map(DependencyVersion::of) //
				.sorted() //
				.collect(Collectors.toList());

		DependencyUpgradeProposal proposal = DependencyOperations.getDependencyUpgradeProposal(Iteration.SR1,
				DependencyVersion.of("5.7.0"), availableVersions);

		assertThat(proposal.getCurrent()).isEqualTo(DependencyVersion.of("5.7.0"));
		assertThat(proposal.getLatestMinor()).isEqualTo(DependencyVersion.of("5.7.1"));
		assertThat(proposal.getProposal()).isEqualTo(DependencyVersion.of("5.7.1"));
		assertThat(proposal.getLatest()).isEqualTo(DependencyVersion.of("5.8.0"));
	}

	@Test
	void shouldSelectLatestVersion() {

		List<DependencyVersion> availableVersions = Stream.of("5.7.0", "5.7.1", "5.8.0") //
				.map(DependencyVersion::of) //
				.sorted() //
				.collect(Collectors.toList());

		DependencyUpgradeProposal proposal = DependencyOperations.getDependencyUpgradeProposal(Iteration.M1,
				DependencyVersion.of("5.7.0"), availableVersions);

		assertThat(proposal.getCurrent()).isEqualTo(DependencyVersion.of("5.7.0"));
		assertThat(proposal.getLatestMinor()).isEqualTo(DependencyVersion.of("5.7.1"));
		assertThat(proposal.getProposal()).isEqualTo(DependencyVersion.of("5.8.0"));
		assertThat(proposal.getLatest()).isEqualTo(DependencyVersion.of("5.8.0"));
	}

	@Test
	void shouldReportNewerVersions() {

		List<DependencyVersion> availableVersions = Stream.of("5.7.0", "5.7.1", "5.7.2-M2", "5.7.2", "5.8.0") //
				.map(DependencyVersion::of) //
				.sorted() //
				.collect(Collectors.toList());

		DependencyUpgradeProposal proposal = DependencyOperations.getDependencyUpgradeProposal(Iteration.SR1,
				DependencyVersion.of("5.7.1"), availableVersions);

		assertThat(proposal.getNewerVersions()).extracting(DependencyVersion::getIdentifier).containsExactly("5.7.2-M2",
				"5.7.2", "5.8.0");
	}

	@Test
	void shouldReportMilestoneVersionForMilestoneIteration() {

		List<DependencyVersion> availableVersions = Stream.of("5.7.0", "5.7.1", "5.7.2-M2") //
				.map(DependencyVersion::of) //
				.sorted() //
				.collect(Collectors.toList());

		DependencyUpgradeProposal proposal = DependencyOperations.getDependencyUpgradeProposal(Iteration.M1,
				DependencyVersion.of("5.7.1"), availableVersions);

		assertThat(proposal.getLatest()).extracting(DependencyVersion::getIdentifier).isEqualTo("5.7.2-M2");
		assertThat(proposal.getProposal()).extracting(DependencyVersion::getIdentifier).isEqualTo("5.7.2-M2");
		assertThat(proposal.getNewerVersions()).extracting(DependencyVersion::getIdentifier).containsExactly("5.7.2-M2");
	}

	@Test
	void shouldSkipMilestoneVersionForNonMilestoneIteration() {

		List<DependencyVersion> availableVersions = Stream.of("5.7.0", "5.7.1", "5.7.2-M2") //
				.map(DependencyVersion::of) //
				.sorted() //
				.collect(Collectors.toList());

		DependencyUpgradeProposal proposal = DependencyOperations.getDependencyUpgradeProposal(Iteration.RC1,
				DependencyVersion.of("5.7.1"), availableVersions);

		assertThat(proposal.getLatest()).extracting(DependencyVersion::getIdentifier).isEqualTo("5.7.1");
		assertThat(proposal.getProposal()).extracting(DependencyVersion::getIdentifier).isEqualTo("5.7.1");
		assertThat(proposal.getNewerVersions()).extracting(DependencyVersion::getIdentifier).containsExactly("5.7.2-M2");
	}
}
