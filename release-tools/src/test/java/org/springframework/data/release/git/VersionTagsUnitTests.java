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
package org.springframework.data.release.git;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.model.Version;

/**
 * Unit tests for {@link VersionTags}.
 * 
 * @author Mark Paluch
 */
public class VersionTagsUnitTests {

	@Test
	public void shouldCreateNewTagFromEmptyTags() {

		VersionTags tags = new VersionTags(Collections.emptyList());
		Module module = Module.create(Projects.BUILD, Version.of(1, 0));

		Tag tag = tags.createTag(new ModuleIteration(module, new TrainIteration(ReleaseTrains.LOVELACE, Iteration.M1)));

		assertThat(tag.toString()).isEqualTo("1.0.0.M1");
	}
}
