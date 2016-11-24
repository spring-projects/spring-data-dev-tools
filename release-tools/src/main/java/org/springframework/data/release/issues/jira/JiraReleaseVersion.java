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

package org.springframework.data.release.issues.jira;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Value;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.util.Assert;

/**
 * Value object to bind REST responses to.
 * 
 * @author Mark Paluch
 */
@Value
class JiraReleaseVersion {

	String id, name, project, description;
	boolean released;

	public static JiraReleaseVersion of(ModuleIteration moduleIteration, JiraVersion jiraVersion) {

		Assert.notNull(moduleIteration, "ModuleIteration must not be null.");
		Assert.notNull(jiraVersion, "JiraVersion must not be null.");

		return new JiraReleaseVersion(null, jiraVersion.toString(), moduleIteration.getProjectKey().getKey(),
				jiraVersion.getDescription(), false);
	}

	public JiraReleaseVersion markReleased() {
		return new JiraReleaseVersion(id, name, project, description, true);
	}

	public boolean hasSameNameAs(JiraVersion jiraVersion) {
		return getName().equals(jiraVersion.toString());
	}

	@JsonIgnore
	public boolean isOpen() {
		return !released;
	}
}
