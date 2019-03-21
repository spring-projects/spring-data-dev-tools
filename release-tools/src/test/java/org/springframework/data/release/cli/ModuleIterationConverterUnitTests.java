/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.release.cli;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Projects;

/**
 * Unit tests for {@link ModuleIterationConverter}.
 *
 * @author Mark Paluch
 */
public class ModuleIterationConverterUnitTests {

	ModuleIterationConverter converter = new ModuleIterationConverter();

	/**
	 * @see #85
	 */
	@Test
	public void shouldResolveModuleIteration() {

		ModuleIteration moduleIteration = converter.convertFromText("Elasticsearch 2.3 M1", ModuleIteration.class, null);

		assertThat(moduleIteration).isNotNull();
		assertThat(moduleIteration.getTrain().getName()).isEqualTo("STANDALONE");
		assertThat(moduleIteration.getTrain().isDetached()).isTrue();
		assertThat(moduleIteration.getProject()).isEqualTo(Projects.ELASTICSEARCH);
		assertThat(moduleIteration.getMediumVersionString()).isEqualTo("2.3 M1");
	}

	/**
	 * @see #85
	 */
	@Test
	public void shouldResolveModuleIterationWithAssociatedTrain() {

		ModuleIteration moduleIteration = converter.convertFromText("Elasticsearch 2.3 Moore M1", ModuleIteration.class,
				null);

		assertThat(moduleIteration).isNotNull();
		assertThat(moduleIteration.getProject()).isEqualTo(Projects.ELASTICSEARCH);
		assertThat(moduleIteration.getTrain().isDetached()).isTrue();
		assertThat(moduleIteration.getTrain().getName()).isEqualTo("Moore");
	}

	/**
	 * @see #85
	 */
	@Test
	public void trainShouldContainOnlySingleModule() {

		ModuleIteration moduleIteration = converter.convertFromText("Elasticsearch 1.0 M1", ModuleIteration.class, null);

		assertThat(moduleIteration.getTrain()).hasSize(1);
	}
}
