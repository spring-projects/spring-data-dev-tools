/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.release.infra;

import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.release.model.Version;

/**
 * Value object representing a dependency version. The primary identifier is {@link #identifier} that corresponds with
 * the actual version identifier. Version identifiers are attempted to be parsed into either a version following Spring
 * Version or SemVer rules ({@code 1.2.3.RELEASE}, {@code 1.2.3-rc1}) or train name with counter rules
 * ({@code Foo-RELEASE}, {@code Foo-SR1}).
 *
 * @author Mark Paluch
 */
@Value
class DependencyVersion implements Comparable<DependencyVersion> {

	private static Pattern VERSION = Pattern.compile("((?>(?>\\d+)[\\.]?)+)((?>-)?[a-zA-Z]+)?(\\d+)?");
	private static Pattern NAME_VERSION = Pattern.compile("([A-Za-z]+)-(RELEASE|SR(\\d+)|SNAPSHOT|BUILD-SNAPSHOT)");

	private static Comparator<DependencyVersion> VERSION_COMPARATOR = Comparator.comparing(DependencyVersion::getVersion)
			.thenComparing((o1, o2) -> {

				// no modifier means release so it's higher order
				if (o1.getModifier().isEmpty() && !o2.getModifier().isEmpty()) {
					return 1;
				}

				if (!o1.getModifier().isEmpty() && o2.getModifier().isEmpty()) {
					return -1;
				}

				return 0;
			}).thenComparing(DependencyVersion::getModifier).thenComparing(DependencyVersion::getCounter)
			.thenComparing(DependencyVersion::getIdentifier);

	private static Comparator<DependencyVersion> TRAIN_VERSION_COMPARATOR = Comparator
			.comparing(DependencyVersion::getTrainName).thenComparing(DependencyVersion::getVersion);

	String identifier;
	String trainName;
	Version version;
	String modifier;
	int counter;
	@With LocalDateTime createdAt;

	public static DependencyVersion of(String identifier) {

		Matcher bomMatcher = NAME_VERSION.matcher(identifier);

		if (bomMatcher.find()) {

			Version version = Version.of(0);
			String modifier = "";
			if (identifier.contains("-SR")) {
				version = Version.of(Integer.parseInt(bomMatcher.group(3)));
			}

			if (identifier.endsWith("-SNAPSHOT")) {
				modifier = "SNAPSHOT";
			}

			return new DependencyVersion(identifier, bomMatcher.group(1), version, modifier, 0, null);
		}

		Matcher versionMatcher = VERSION.matcher(identifier);

		if (versionMatcher.find()) {
			Version version = null;
			String modifier;
			String counter;
			String versionString = versionMatcher.group(1);

			versionString = versionString.endsWith(".") ? versionString.substring(0, versionString.length() - 1)
					: versionString;
			try {
				version = Version.parse(versionString);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException(String.format("Cannot parse version number %s", versionString), e);
			}

			modifier = versionMatcher.group(2);
			counter = versionMatcher.group(3);

			return new DependencyVersion(identifier, null, version, modifier == null ? "" : modifier.toUpperCase(Locale.ROOT),
					counter != null ? Integer.parseInt(counter) : 0, null);
		}

		throw new IllegalArgumentException(String.format("Cannot parse version identifier %s", identifier));
	}

	public boolean isNewer(DependencyVersion other) {
		return this.compareTo(other) > 0;
	}

	@Override
	public int compareTo(DependencyVersion o) {

		if (trainName != null && o.trainName != null) {
			return TRAIN_VERSION_COMPARATOR.compare(this, o);
		}

		if (trainName != null) {
			return -1;
		}

		if (o.trainName != null) {
			return 1;
		}

		if (version != null && o.version != null) {
			return VERSION_COMPARATOR.compare(this, o);
		}

		return identifier.compareTo(o.identifier);
	}

	@Override
	public String toString() {
		return identifier;
	}
}
