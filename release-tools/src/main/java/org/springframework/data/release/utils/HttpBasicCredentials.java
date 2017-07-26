/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.release.utils;

import lombok.NonNull;
import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.data.release.model.Password;

/**
 * @author Oliver Gierke
 */
@Value
public class HttpBasicCredentials {

	private final @NonNull String username;
	private final @NonNull Password password;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		String header = username.concat(":").concat(password.toString());
		byte[] encodedAuth = Base64.getEncoder().encode(header.getBytes(StandardCharsets.US_ASCII));

		return "Basic ".concat(new String(encodedAuth));
	}
}
