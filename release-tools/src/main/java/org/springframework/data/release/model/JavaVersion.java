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
package org.springframework.data.release.model;

import lombok.Value;

import java.util.function.Predicate;

/**
 * Value object representing a Java version.
 *
 * @author Mark Paluch
 */
@Value(staticConstructor = "of")
public class JavaVersion {

	public static final JavaVersion JAVA_8 = of("Java 1.8",
			version -> version.getMajor() == 1 && version.getMinor() == 8);

	public static final JavaVersion JAVA_17 = of("Java 17", version -> version.getMajor() == 17);

	String name;
	Predicate<Version> versionDetector;

}
