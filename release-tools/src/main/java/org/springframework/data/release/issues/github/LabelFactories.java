/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.release.issues.github;

import java.util.function.BiFunction;

public class LabelFactories {

	public static final LabelFactory FOR_LABEL = (name, description) -> create("for:", name, description, "c5def5");

	public static final LabelFactory STATUS_LABEL = (name, description) -> create("status:", name, description, "fef2c0");

	public static final LabelFactory IN_LABEL = (name, description) -> create("in:", name, description, "e8f9de");

	public static final LabelFactory TYPE_LABEL = (name, description) -> create("type:", name, description, "e3d9fc");

	public static final LabelFactory HAS_LABEL = (name, description) -> create("has:", name, description, "dfdfdf");

	private static Label create(String prefix, String labelName, String description, String color) {
		return new Label(String.format("%s %s", prefix, labelName), description, color);
	}

	interface LabelFactory extends BiFunction<String, String, Label> {

	}

}
