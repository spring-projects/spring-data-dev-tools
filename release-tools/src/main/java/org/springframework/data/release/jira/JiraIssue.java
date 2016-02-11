/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.release.jira;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.release.model.Train;

/**
 * Value object to bind REST responses to.
 * 
 * @author Oliver Gierke
 */
@Data
class JiraIssue {

	private String key;
	private Fields fields;

	/**
	 * Returns whether the ticket is only fixed in the given {@link Train}. {@literal false} indicates the {@link Ticket}
	 * has been resolved for
	 * 
	 * @param train
	 * @return
	 */
	public boolean wasBackportedFrom(Train train) {

		List<FixVersions> fixVersions = fields.getFixVersions();
		return !(fixVersions.size() == 1 && fixVersions.get(0).name.contains(train.getName()));
	}

	@Data
	static class Fields {

		String summary;
		List<FixVersions> fixVersions;
		Status status;
		Resolution resolution;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class FixVersions {

		String name;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Status {

		String name;
		StatusCategory statusCategory;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Resolution {

		String name;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class StatusCategory {

		String key;
	}
}
