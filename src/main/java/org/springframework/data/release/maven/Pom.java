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

import org.xmlbeam.annotation.XBRead;
import org.xmlbeam.annotation.XBWrite;

/**
 * @author Oliver Gierke
 */
public interface Pom {

	@XBRead("/project")
	Artifact getArtifactId();

	@XBRead("/project/version")
	String getVersion();

	@XBWrite("/project/version")
	void setVersion(String version);

	@XBRead("/project/repositories/repository[id=\"spring-libs-snapshot\"]")
	Repository getSpringRepository();

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
