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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.maven.shared.utils.StringUtils;

/**
 * Value upgrade capturing {@link DependencyVersion} for a {@link Dependency}.
 *
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DependencyVersions {

	@Getter Map<Dependency, DependencyVersion> versions;

	public static DependencyVersions empty() {
		return new DependencyVersions(Collections.emptyMap());
	}

	/**
	 * Returns whether there's a version specified for {@link Dependency}.
	 *
	 * @param dependency
	 * @return
	 */
	public boolean hasDependency(Dependency dependency) {
		return versions.containsKey(dependency);
	}

	/**
	 * Returns the {@link DependencyVersion} for {@link Dependency} or throws {@link IllegalArgumentException} if no
	 * version was specified. It's recommended to check whether a version has been specified using
	 * {@link #hasDependency(Dependency)}.
	 *
	 * @param dependency
	 * @return
	 * @see #hasDependency(Dependency)
	 */
	public DependencyVersion get(Dependency dependency) {

		if (!hasDependency(dependency)) {
			throw new IllegalArgumentException(String.format("No such dependency: %s", dependency));
		}

		return versions.get(dependency);
	}

	/**
	 * Apply the {@code action} to all dependeny/dependency version pairs.
	 *
	 * @param action
	 */
	public void forEach(BiConsumer<Dependency, DependencyVersion> action) {
		versions.forEach(action);
	}

	/**
	 * Returns whether dependency versions are configured.
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return versions.isEmpty();
	}

	public Collection<Dependency> getDependencies() {
		return versions.keySet();
	}

	@Override
	public String toString() {
		return toString(0);
	}

	public String toString(int indentation) {

		StringBuilder result = new StringBuilder();

		for (Map.Entry<Dependency, DependencyVersion> entry : versions.entrySet()) {

			result.append(StringUtils.repeat("\t", indentation)).append(StringUtils.rightPad(entry.getKey().toString(), 40))
					.append(" = ").append(entry.getValue()).append(System.lineSeparator());
		}

		return result.toString();
	}
}
