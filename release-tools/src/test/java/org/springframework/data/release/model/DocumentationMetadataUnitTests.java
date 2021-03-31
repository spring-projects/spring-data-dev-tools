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
package org.springframework.data.release.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DocumentationMetadata}.
 *
 * @author Mark Paluch
 */
class DocumentationMetadataUnitTests {

	@Test // gh-167
	void shouldReportCorrectDocumentationUrls() {

		DocumentationMetadata metadata = DocumentationMetadata.of(Projects.MONGO_DB, ArtifactVersion.of("3.1.0"));

		assertThat(metadata.getApiDocUrl(ReleaseTrains.OCKHAM))
				.isEqualTo("https://docs.spring.io/spring-data/mongodb/docs/3.1.0/api/");
		assertThat(metadata.getReferenceDocUrl(ReleaseTrains.OCKHAM))
				.isEqualTo("https://docs.spring.io/spring-data/mongodb/docs/3.1.0/reference/html/");
	}

	@Test // gh-167
	void shouldReportCorrectCommonsDocumentationUrlsForBuildProject() {

		DocumentationMetadata metadata = DocumentationMetadata.of(Projects.BUILD, ArtifactVersion.of("3.1.0"));

		assertThat(metadata.getApiDocUrl(ReleaseTrains.OCKHAM))
				.isEqualTo("https://docs.spring.io/spring-data/commons/docs/3.1.0/api/");
		assertThat(metadata.getReferenceDocUrl(ReleaseTrains.OCKHAM))
				.isEqualTo("https://docs.spring.io/spring-data/commons/docs/3.1.0/reference/html/");
	}
}
