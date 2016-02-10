/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.release.deployment;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oliver Gierke
 */
@Data
public class Errors {

	private List<Error> errors = new ArrayList<>();
	private List<Message> messages = new ArrayList<>();

	public List<Error> getErrors(Errors this) {
		return errors;
	}

	@Data
	static class Error {

		private String message;
		private int status;

		public String toString() {
			return String.format("%s - %s", status, message);
		}
	}

	@Data
	static class Message {

		private String level, message;

		public String toString() {
			return String.format("%s - %s", level, message);
		}
	}
}
