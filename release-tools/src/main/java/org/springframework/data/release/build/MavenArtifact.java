/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.springframework.data.release.model.Projects.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.util.Assert;

/**
 * Value object to represent a Maven artifact.
 * 
 * @author Oliver Gierke
 */
@EqualsAndHashCode
public class MavenArtifact {

	private static final GroupId GROUP_ID = GroupId.of("org.springframework.data");

	private final Project project;
	private final @Getter Repository repository;
	private final @Getter ArtifactVersion version;

	/**
	 * Creates a new {@link MavenArtifact} for the given {@link ModuleIteration}.
	 * 
	 * @param module must not be {@literal null}.
	 */
	public MavenArtifact(ModuleIteration module) {

		Assert.notNull(module, "Module iteration must not be null!");

		this.project = module.getModule().getProject();
		this.repository = new Repository(module.getIteration());
		this.version = ArtifactVersion.of(module);
	}

	public MavenArtifact(Project project, ArtifactVersion version) {

		this.project = project;
		this.repository = new Repository(version);
		this.version = version;
	}

	public String getGroupId() {
		return GROUP_ID.getValue();
	}

	/**
	 * Returns the Maven artifact identifier.
	 * 
	 * @return
	 */
	public String getArtifactId() {

		String artifactId = String.format("spring-data-%s", project.getName().toLowerCase());

		return REST.equals(project) ? artifactId.concat("-webmvc") : artifactId;
	}

	public ArtifactVersion getNextDevelopmentVersion() {
		return version.getNextDevelopmentVersion();
	}

	/**
	 * Returns the URL pointing to the artifacts.
	 * 
	 * @return
	 */
	public String getRootUrl() {
		return String.format("%s/%s/%s/%s", repository.getUrl(), GROUP_ID.asPath(), getArtifactId(), version);
	}
}
