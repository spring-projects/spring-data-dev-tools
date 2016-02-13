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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import org.springframework.data.release.model.IterationVersion;
import org.springframework.data.release.model.Version;
import org.springframework.data.release.model.VersionAware;
import org.springframework.util.Assert;

/**
 * Value type to represent an SCM branch.
 * 
 * @author Oliver Gierke
 */
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class Branch {

	public static final Branch MASTER = new Branch("master");

	private final String name;

	/**
	 * Creates a new {@link Branch} from the given {@link IterationVersion}.
	 * 
	 * @param iterationVersion must not be {@literal null}.
	 * @return
	 */
	public static Branch from(IterationVersion iterationVersion) {

		Assert.notNull(iterationVersion, "Iteration version must not be null!");

		if (iterationVersion.isServiceIteration()) {
			return from((VersionAware) iterationVersion);
		}

		return MASTER;
	}

	public static Branch from(VersionAware versioned) {
		return from(versioned.getVersion());
	}

	private static Branch from(Version version) {
		return from(version.toString().concat(".x"));
	}

	public static Branch from(String name) {

		int slashIndex = name.lastIndexOf('/');

		return new Branch(slashIndex != -1 ? name.substring(slashIndex + 1) : name);
	}

	public boolean isMasterBranch() {
		return MASTER.equals(this);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
}
