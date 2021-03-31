/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.release.build;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.Assume.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Projects;

import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.evaluation.XPathEvaluator;
import org.xmlbeam.io.XBFileIO;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class MavenIntegrationTests extends AbstractIntegrationTests {

	@Autowired Workspace workspace;
	@Autowired ProjectionFactory projection;
	@Autowired MavenBuildSystem maven;
	@Autowired GitOperations git;

	@BeforeAll
	static void beforeClass() {

		try {
			URL url = new URL("https://github.com");
			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
			urlConnection.getInputStream().close();
		} catch (IOException e) {
			assumeTrue("Test requires connectivity to GitHub:" + e.toString(), false);
		}
	}

	@Test
	void modifiesParentPomCorrectly() throws IOException {

		XBFileIO io = projection.io().file(new ClassPathResource("parent-pom.xml").getFile());

		ParentPom pom = io.read(ParentPom.class);
		pom.setSharedResourcesVersion(ArtifactVersion.of("1.2.0.RELEASE"));

		String xml = projection.asString(pom);
		XPathEvaluator xPathEvaluator = projection.io().stream(new ByteArrayInputStream(xml.getBytes())).evalXPath(
				"/project/profiles/profile[id=\"distribute\"]/dependencies/dependency[artifactId=\"spring-data-build-resources\"]/version");

		assertThat(xPathEvaluator.asString()).isEqualToIgnoringCase("1.2.0.RELEASE");
	}

	@Test
	void updatesRepositoriesCorrectly() throws Exception {

		XBFileIO io = projection.io().file(new ClassPathResource("sample-pom.xml").getFile());

		Pom pom = io.read(Pom.class);

		pom.setRepositoryId("spring-libs-snapshot", "spring-libs-release");
		pom.setRepositoryUrl("spring-libs-release", "https://repo.spring.io/libs-release");
	}

	@Test
	void findsSnapshotDependencies() throws Exception {

		File file = workspace.getFile("pom.xml", Projects.BUILD);

		assumeThat(file).exists();

		Pom pom = projection.io().file(file).read(Pom.class);
	}
}
