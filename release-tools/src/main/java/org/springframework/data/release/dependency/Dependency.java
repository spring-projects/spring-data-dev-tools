/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.dependency;

import lombok.Value;

import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * @author Mark Paluch
 */
@Value
public class Dependency implements Comparable<Dependency> {

	String name;
	String groupId, artifactId;
	Predicate<String> exclusions;

	public static Dependency of(String name, String ga) {

		Assert.hasText(name, "Name must not be empty");
		Assert.hasText(ga, "GroupId/ArtifactId must not be empty");
		Assert.isTrue(ga.indexOf(':') != -1, "GroupId/ArtifactId must be in the format of org.group:artifact-id");

		String[] parts = ga.split(":");

		return new Dependency(name, parts[0], parts[1], it -> false);
	}

	public Dependency excludeVersionStartingWith(String identifier) {
		return new Dependency(name, groupId, artifactId, it -> it.startsWith(identifier) || exclusions.test(it));
	}

	public boolean shouldInclude(String identifier) {
		return !exclusions.test(identifier);
	}

	@Override
	public int compareTo(Dependency o) {
		return name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return getGroupId() + ":" + getArtifactId();
	}
}
