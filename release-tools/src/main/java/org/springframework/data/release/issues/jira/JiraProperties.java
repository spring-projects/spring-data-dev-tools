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

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Configurable properties for JIRA.
 *
 * @author Mark Paluch
 */
@Data
@Component
@ConfigurationProperties(prefix = "jira")
class JiraProperties {

	private @Getter(AccessLevel.PRIVATE) String password;
	private String username, apiUrl;

	@PostConstruct
	public void init() {

		Assert.hasText(username, "No Jira username (jira.username) configured!");
		Assert.hasText(password, "No Jira password (jira.password) configured!");
		Assert.hasText(apiUrl, "No Jira url (jira.api-url) configured!");
	}

	/**
	 * Returns the {@link Credentials} to be used.
	 *
	 * @return
	 */
	public Credentials getCredentials() {
		return new Credentials(username, password);
	}
}
