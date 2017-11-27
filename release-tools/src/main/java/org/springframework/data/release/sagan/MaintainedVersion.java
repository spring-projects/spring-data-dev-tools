/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.release.sagan;

import lombok.Value;

import java.util.Comparator;
import java.util.stream.Stream;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Train;
import org.springframework.util.Assert;

/**
 * Represents a maintained project version to be represented in Sagan.
 * 
 * @author Oliver Gierke
 */
@Value(staticConstructor = "of")
class MaintainedVersion implements Comparable<MaintainedVersion> {

	Project project;
	ArtifactVersion version;
	Train train;

	/**
	 * Creates a new {@link MaintainedVersion} representing the snaphot version of the given {@link Module} and
	 * {@link Train}.
	 * 
	 * @param module must not be {@literal null}.
	 * @param train must not be {@literal null}.
	 * @return
	 */
	public static MaintainedVersion snapshot(Module module, Train train) {

		Assert.notNull(module, "Module must not be null!");
		Assert.notNull(train, "Train must not be null!");

		ArtifactVersion snapshotVersion = ArtifactVersion.of(module.getVersion()).getSnapshotVersion();

		return MaintainedVersion.of(module.getProject(), snapshotVersion, train);
	}

	/**
	 * Returns a {@link Stream} of {@link MaintainedVersion} that are related to the current one. In case the current one
	 * is not a snapshot version in the first place, the next development version is added, too.
	 * 
	 * @return
	 */
	Stream<MaintainedVersion> all() {
		return version.isSnapshotVersion() ? Stream.of(this) : Stream.of(nextDevelopmentVersion(), this);
	}

	/**
	 * Creates the {@link MaintainedVersion} for the next development version of the current one.
	 * 
	 * @return
	 */
	MaintainedVersion nextDevelopmentVersion() {
		return MaintainedVersion.of(project, version.getNextBugfixVersion(), train);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MaintainedVersion o) {
		return Comparator.comparing(MaintainedVersion::getVersion).compare(this, o);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("%s - %s - %s", project.getName(), train.getName(), version);
	}
}
