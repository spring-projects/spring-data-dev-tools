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
package org.springframework.data.release.git;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import org.springframework.data.release.model.ArtifactVersion;

/**
 * Value object to represent an SCM tag.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class Tag implements Comparable<Tag> {

	private final String name;

	/**
	 * Returns the part of the name of the tag that is suitable to derive a version from the tag. Will transparently strip
	 * a {@code v} prefix from the name.
	 * 
	 * @return
	 */
	private String getVersionSource() {
		return name.startsWith("v") ? name.substring(1) : name;
	}

	public ArtifactVersion toArtifactVersion() {
		return ArtifactVersion.parse(getVersionSource());
	}

	/**
	 * Creates a new {@link Tag} for the given {@link ArtifactVersion} based on the format of the current one.
	 * 
	 * @param version
	 * @return
	 */
	public Tag createNew(ArtifactVersion version) {
		return new Tag(name.startsWith("v") ? "v".concat(version.toString()) : version.toString());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Tag that) {
		return that.name.compareTo(this.name);
	}
}
