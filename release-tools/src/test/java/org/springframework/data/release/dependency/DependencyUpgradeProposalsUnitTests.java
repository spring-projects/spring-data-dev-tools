/*
 * Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ReleaseTrains;

/**
 * Unit tests for {@link DependencyUpgradeProposals}.
 *
 * @author Mark Paluch
 */
class DependencyUpgradeProposalsUnitTests {

	@Test
	void shouldParseDependencies() {

		Properties properties = new Properties();
		properties.put("dependency.train", "Pascal");
		properties.put("dependency.iteration", "M1");
		properties.put("dependency.upgrade.count", "2");
		properties.put("dependency[org.assertj:assertj-core]", "3.18.1");
		properties.put("dependency[io.reactivex.rxjava3:rxjava]", "1.2.3");

		DependencyVersions dependencies = DependencyUpgradeProposals
				.fromProperties(ReleaseTrains.PASCAL.getIteration(Iteration.M1), properties);

		assertThat(dependencies.getVersions()).hasSize(2)
				.containsEntry(Dependencies.ASSERTJ, DependencyVersion.of("3.18.1"))
				.containsEntry(Dependencies.RXJAVA3, DependencyVersion.of("1.2.3"));
	}
}
