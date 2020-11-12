/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.issues.github;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * GitHub label.
 *
 * @author Mark Paluch
 */
@Value
@EqualsAndHashCode(of = "name")
public class Label {

	String name;

	String description;

	String color;

	public boolean requiresUpdate(Label other) {

		if (!other.getName().equals(getName())) {
			return true;
		}

		if (!other.getDescription().equals(getDescription())) {
			return true;
		}

		if (!other.getColor().equals(getColor())) {
			return true;
		}

		return false;
	}
}
