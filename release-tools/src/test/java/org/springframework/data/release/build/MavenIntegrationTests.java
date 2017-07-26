/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.release.build;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.TrainIteration;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 */
public class MavenIntegrationTests extends AbstractIntegrationTests {

	@Autowired Workspace workspace;
	@Autowired ProjectionFactory projection;
	@Autowired MavenBuildSystem maven;
	@Autowired GitOperations git;

	@Test
	public void modifiesParentPomCorrectly() throws IOException {

		XBFileIO io = projection.io().file(new ClassPathResource("parent-pom.xml").getFile());

		ParentPom pom = io.read(ParentPom.class);
		pom.setSharedResourcesVersion(ArtifactVersion.of("1.2.0.RELEASE"));

		System.out.println(projection.asString(pom));
	}

	@Test
	public void updatesRepositoriesCorrectly() throws Exception {

		XBFileIO io = projection.io().file(new ClassPathResource("sample-pom.xml").getFile());

		Pom pom = io.read(Pom.class);

		pom.setRepositoryId("spring-libs-snapshot", "spring-libs-release");
		pom.setRepositoryUrl("spring-libs-release", "https://repo.spring.io/libs-release");

		// System.out.println(projection.asString(pom));
	}

	@Test
	public void testname() throws Exception {

		git.update(ReleaseTrains.HOPPER);

		TrainIteration iteration = new TrainIteration(ReleaseTrains.HOPPER, Iteration.M1);
		ModuleIteration build = iteration.getModule(Projects.BUILD);
		UpdateInformation information = UpdateInformation.of(iteration, Phase.PREPARE);

		maven.updateProjectDescriptors(build, information);
		maven.prepareVersion(build, Phase.PREPARE);
	}

	@Test
	public void findsSnapshotDependencies() throws Exception {

		Pom pom = projection.io().file(workspace.getFile("bom/pom.xml", Projects.BUILD)).read(Pom.class);

		System.out.println(pom.getSnapshotDependencies());
	}
}
