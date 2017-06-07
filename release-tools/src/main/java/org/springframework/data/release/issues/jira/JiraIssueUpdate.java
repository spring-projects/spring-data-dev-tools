package org.springframework.data.release.issues.jira;

import lombok.NonNull;
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
class JiraIssueUpdate {

	private final Map<String, Object> update;

	private JiraIssueUpdate(Map<String, Object> update) {
		this.update = update;
	}

	/**
	 * Create an empty {@link JiraIssueUpdate}.
	 *
	 * @return
	 */
	public static JiraIssueUpdate create() {
		return new JiraIssueUpdate(Collections.emptyMap());
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

		return new JiraIssueUpdate(update);
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
