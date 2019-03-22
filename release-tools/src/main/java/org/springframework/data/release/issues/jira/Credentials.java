/*
 * Copyright 2014-2016 the original author or authors.
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
package org.springframework.data.release.issues.jira;

import lombok.Value;

import javax.xml.bind.DatatypeConverter;

/**
 * @author Oliver Gierke
 */
@Value
class Credentials {

	String username, password;

	public String asBase64() {
		return DatatypeConverter.printBase64Binary(String.format("%s:%s", username, password).getBytes());
	}
}
