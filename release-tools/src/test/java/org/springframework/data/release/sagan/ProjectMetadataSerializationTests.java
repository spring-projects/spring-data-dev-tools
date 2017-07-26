/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.release.sagan;

import org.junit.Test;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Tests for serialization of {@link ProjectMetadata}.
 * 
 * @author Oliver Gierke
 */
public class ProjectMetadataSerializationTests {

	@Test
	public void serializesMaintainedVersionsIntoProjectMetadata() throws Exception {

		ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();

		MaintainedVersion kay = MaintainedVersion.of(Projects.COMMONS, ArtifactVersion.of("2.0.0.RC1"), ReleaseTrains.KAY);
		MaintainedVersion ingalls = MaintainedVersion.of(Projects.COMMONS, ArtifactVersion.of("1.13.5.RELEASE"),
				ReleaseTrains.INGALLS);
		MaintainedVersion ingallsSnapshot = ingalls.nextDevelopmentVersion();
		MaintainedVersion hopper = MaintainedVersion.of(Projects.COMMONS, ArtifactVersion.of("1.12.8.RELEASE"),
				ReleaseTrains.HOPPER);

		MaintainedVersions versions = MaintainedVersions.of(kay, ingalls, ingallsSnapshot, hopper);

		System.out.println(mapper.writeValueAsString(new ProjectMetadata(kay, versions)));
		System.out.println(mapper.writeValueAsString(new ProjectMetadata(ingallsSnapshot, versions)));
		System.out.println(mapper.writeValueAsString(new ProjectMetadata(ingalls, versions)));
		System.out.println(mapper.writeValueAsString(new ProjectMetadata(hopper, versions)));
	}
}
