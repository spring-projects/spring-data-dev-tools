/*
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.release.Streamable;
import org.springframework.data.release.model.ArtifactVersion;

/**
 * Wrapper type for a {@link List} of {@link MaintainedVersion}.
 * 
 * @author Oliver Gierke
 */
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class MaintainedVersions implements Streamable<MaintainedVersion> {

	private final List<MaintainedVersion> versions;

	/**
	 * Creates a new {@link MaintainedVersions} with the given {@link MaintainedVersion}s in descending order (more recent
	 * versions first).
	 * 
	 * @param versions must not be {@literal null}.
	 * @return
	 */
	static MaintainedVersions of(List<MaintainedVersion> versions) {

		return new MaintainedVersions(versions.stream()//
				.sorted(Comparator.reverseOrder())//
				.collect(Collectors.toList()));
	}

	/**
	 * Creates a new {@link MaintainedVersions} with the given {@link MaintainedVersion}s in descending order (more recent
	 * versions first).
	 * 
	 * @param versions must not be {@literal null}.
	 * @return
	 */
	static MaintainedVersions of(MaintainedVersion... versions) {
		return MaintainedVersions.of(Arrays.asList(versions));
	}

	/**
	 * Returns whether the given {@link MaintainedVersion} is the main version of the current set.
	 * 
	 * @param version must not be {@literal null}.
	 * @return
	 */
	boolean isMainVersion(MaintainedVersion version) {

		if (!version.getVersion().isReleaseVersion()) {
			return false;
		}

		return versions.stream() //
				.map(it -> it.getVersion()) //
				.filter(ArtifactVersion::isReleaseVersion) //
				.findFirst() //
				.map(it -> it.equals(version.getVersion())) //
				.orElse(false);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<MaintainedVersion> iterator() {
		return versions.iterator();
	}
}
