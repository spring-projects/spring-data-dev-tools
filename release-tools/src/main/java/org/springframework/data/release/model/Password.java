/*
 * Copyright 2017 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import org.springframework.util.Assert;

/**
 * Value object to represent a password.
 * 
 * @author Oliver Gierke
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Password implements Masked {

	public static Password NONE = new Password("");

	@Getter(AccessLevel.NONE) String value;

	/**
	 * Create a new {@link Password} for the given value.
	 * 
	 * @param password
	 * @return
	 */
	public static Password of(String password) {

		Assert.hasText(password, "Password must not be null or empty!");
		return new Password(password);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return value;
	}
}
