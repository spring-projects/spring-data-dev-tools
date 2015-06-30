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
package org.springframework.data.release.maven;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 */
public class MavenIntegrationTests extends AbstractIntegrationTests {

	@Autowired Workspace workspace;
	@Autowired ProjectionFactory projection;

	public @Rule TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void modifiesParentPomCorrectly() throws IOException {

		XBFileIO io = projection.io().file(new ClassPathResource("parent-pom.xml").getFile());

		ParentPom pom = io.read(ParentPom.class);
		pom.setSharedResourcesVersion(ArtifactVersion.parse("1.2.0.RELEASE"));

		// System.out.println(projection.asString(pom));
	}

	@Test
	public void updatesRepositoriesCorrectly() throws Exception {

		XBFileIO io = projection.io().file(new ClassPathResource("sample-pom.xml").getFile());

		Pom pom = io.read(Pom.class);

		pom.setRepositoryId("spring-libs-snapshot", "spring-libs-release");
		pom.setRepositoryUrl("spring-libs-release", "https://repo.spring.io/libs-release");

		// System.out.println(projection.asString(pom));
	}
}
