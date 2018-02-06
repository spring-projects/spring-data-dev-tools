/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.release.issues.jira;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Value object to represent a Jira issue update via {@code PUT} with {@code update} fields.
 *
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class JiraIssueUpdate {

	private final Map<String, Object> update;
	private final Map<String, Object> transition;

	/**
	 * Create an empty {@link JiraIssueUpdate}.
	 *
	 * @return
	 */
	public static JiraIssueUpdate create() {
		return new JiraIssueUpdate(Collections.emptyMap(), Collections.emptyMap());
	}

	/**
	 * Assign the issue to {@code userId}.
	 *
	 * @param userId must not be {@literal null} or empty.
	 * @return
	 */
	public JiraIssueUpdate assignTo(String userId) {

		Assert.hasText(userId, "UserId must not be null or empty!");

		Map<String, Object> update = new LinkedHashMap<>(this.update);
		update.put("assignee", new AssignTo(userId));

		return new JiraIssueUpdate(update, this.transition);
	}

	/**
	 * Assign the issue to {@code userId}.
	 *
	 * @param transitionId Jira transition Id
	 * @return
	 */
	public JiraIssueUpdate transition(int transitionId) {

		Map<String, Object> transition = new LinkedHashMap<>(this.transition);
		transition.put("id", transitionId);

		return new JiraIssueUpdate(this.update, transition);
	}

	/**
	 * Serializes as [ {"set": "value"} ]
	 */
	@Value
	@JsonSerialize(using = SetSerializer.class)
	static class AssignTo {
		final @NonNull String value;
	}

	static class SetSerializer extends JsonSerializer<AssignTo> {

		@Override
		public void serialize(AssignTo value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

			gen.writeStartArray();

			gen.writeObject(Collections.singletonMap("set", Collections.singletonMap("name", value.getValue())));

			gen.writeEndArray();
		}
	}
}
