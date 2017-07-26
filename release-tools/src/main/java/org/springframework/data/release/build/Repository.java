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
package org.springframework.data.release.build;

import lombok.Value;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */

@Value
public class Repository {

	private static final String ID_BASE = "spring-libs-";
	private static final String BASE = "https://repo.spring.io/libs-";

	String id, url;

	public Repository(Iteration iteration) {

		Assert.notNull(iteration, "Iteration must not be null!");

		this.id = ID_BASE.concat(iteration.isPublic() ? "release" : "milestone");
		this.url = BASE.concat(iteration.isPublic() ? "release" : "milestone");
	}

	public Repository(ArtifactVersion version) {

		String suffix = getSuffixFor(version);

		this.id = ID_BASE.concat(suffix);
		this.url = BASE.concat(suffix);
	}

	public String getSnapshotId() {
		return ID_BASE.concat("snapshot");
	}

	public String getSnapshotUrl() {
		return BASE.concat("snapshot");
	}

	private static String getSuffixFor(ArtifactVersion version) {

		if (version.isSnapshotVersion()) {
			return "snapshot";
		}

		if (version.isMilestoneVersion()) {
			return "milestone";
		}

		if (version.isReleaseVersion()) {
			return "release";
		}

		throw new IllegalArgumentException(String.format("Unsupported ArtifactVersion %s!", version));
	}
}
