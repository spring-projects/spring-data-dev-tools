/*
 * Copyright 2014-2022 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.git.Tag;
import org.springframework.data.release.git.VersionTags;
import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RequiredArgsConstructor
public class StaticResources {

	private static final String URL_TEMPLATE = "https://docs.spring.io/spring-data/%s/docs/%s";

	private final String baseUrl;

	private final String releaseUrl;

	public StaticResources(ModuleIteration module) {

		Project project = module.getProject();
		ArtifactVersion version = ArtifactVersion.of(module);

		this.baseUrl = String.format(URL_TEMPLATE, project.getName().toLowerCase(), version);

		GitProject gitProject = GitProject.of(project);

		Tag tag = VersionTags.empty(module.getProject()).createTag(module);

		this.releaseUrl = String.format("%s/releases/tag/%s", gitProject.getProjectUri(), tag.getName());
	}

	public String getDocumentationUrl() {
		return baseUrl.concat("/reference/html");
	}

	public String getJavaDocUrl() {
		return baseUrl.concat("/api");
	}

	public String getChangelogUrl() {
		return releaseUrl;
	}
}
