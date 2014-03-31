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

import org.springframework.data.release.model.ArtifactVersion;
import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.annotation.XBValue;
import org.xmlbeam.annotation.XBWrite;

/**
 * @author Oliver Gierke
 */
public interface Pom {

	@XBRead("/project")
	Artifact getArtifactId();

	@XBRead("/project/version")
	ArtifactVersion getVersion();

	@XBWrite("/project/version")
	void setVersion(String version);

	@XBRead("/project/repositories/repository[id=\"spring-libs-snapshot\"]")
	Repository getSpringRepository();

	@XBWrite("/project/parent/version")
	void setParentVersion(ArtifactVersion version);

	@XBWrite("/project/properties/{0}")
	void setProperty(String property, @XBValue ArtifactVersion value);

	@XBWrite("/project/repositories/repository[id={0}]/id")
	void setRepositoryId(String oldId, @XBValue String newId);

	@XBWrite("/project/repositories/repository[id={0}]/url")
	void setRepositoryUrl(String id, @XBValue String url);

	public interface Repository {

		@XBRead("child::id")
		String getId();

		@XBRead("child::url")
		String getUrl();
	}

	public interface Artifact {

		@XBRead("child::groupId")
		String getGroupId();

		@XBRead("child::artifactId")
		String getArtifactId();

		@XBRead("child::version")
		String getVersion();
	}
}
