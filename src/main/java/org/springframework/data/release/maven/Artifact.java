/*
 * Copyright 2014 the original author or authors.
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

import lombok.Getter;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.util.Assert;

/**
 * Value object to represent a Maven {@link Artifact}.
 * 
 * @author Oliver Gierke
 */
public class Artifact {

	private static final GroupId GROUP_ID = new GroupId("org.springframework.data");

	private final ModuleIteration module;
	private final Repository repository;
	private final @Getter ArtifactVersion version;

	/**
	 * Creates a new {@link Artifact} for the given {@link ModuleIteration}.
	 * 
	 * @param module must not be {@literal null}.
	 */
	public Artifact(ModuleIteration module) {

		Assert.notNull(module, "Module iteration must not be null!");

		this.module = module;
		this.repository = new Repository(module.getIteration());
		this.version = ArtifactVersion.from(module);
	}

	/**
	 * Returns the Maven artifact identifier.
	 * 
	 * @return
	 */
	public String getArtifactId() {

		String artifactId = String.format("spring-data-%s", module.getProject().getName().toLowerCase());
		return ReleaseTrains.REST.equals(module.getProject()) ? artifactId.concat("-webmvc") : artifactId;
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
