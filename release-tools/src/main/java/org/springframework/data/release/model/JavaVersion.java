/*
 * Copyright 2022-2022 the original author or authors.
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
package org.springframework.data.release.model;

import lombok.Value;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object representing a Java version.
 *
 * @author Mark Paluch
 */
@Value(staticConstructor = "of")
public class JavaVersion {

	private static final Pattern DOCKER_TAG_PATTERN = Pattern.compile("((:?\\d+(:?u\\d+)?(:?\\.\\d+)*)).*");

	public static final JavaVersion JAVA_8 = of("1.8.0_312");

	public static final JavaVersion JAVA_17 = of("Java 17", version -> version.getMajor() == 17, it -> true);

	String name;
	Predicate<Version> versionDetector;
	Predicate<String> implementor;

	public static JavaVersion of(String version) {
		Version expectedVersion = parse(version);
		return of("Java " + version, candidate -> candidate.is(expectedVersion), it -> true);
	}

	public static Version parse(String version) {
		return Version.parse(version.replace('_', '.'));
	}

	/**
	 * Parse a docker tag into a Java version using {@code eclipse-temurin} conventions.
	 *
	 * @param tagName
	 * @return
	 */
	public static JavaVersion fromDockerTag(String tagName) {

		Pattern versionExtractor = Pattern.compile("(:?\\d+(:?u\\d+)?(:?\\.\\d+)*).*");
		Matcher matcher = versionExtractor.matcher(tagName);
		if (!matcher.find()) {
			throw new IllegalStateException(String.format("Cannot parse Java version '%s'", tagName));
		}

		String plainVersion = matcher.group(1);

		if (plainVersion.startsWith("8u")) {
			return of("1.8.0_" + plainVersion.substring(2));
		}

		return of(plainVersion).withImplementor("Temurin");
	}

	public JavaVersion withImplementor(String implementor) {
		return of(String.format("%s (%s)", name, implementor), versionDetector, it -> it.contains(implementor));
	}
}
