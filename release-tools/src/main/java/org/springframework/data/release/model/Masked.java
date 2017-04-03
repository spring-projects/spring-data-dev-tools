/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.release.model;

/**
 * Interface to exposes a method {@link #masked()} to hide an actual value (usually for security reasons).
 * 
 * @author Oliver Gierke
 */
public interface Masked {

	/**
	 * Returns a masked {@link String} as well as information about the type whose String representation was masked.
	 * 
	 * @return
	 */
	default String masked() {
		return String.format("******** (%s)", getClass().getSimpleName());
	}
}
