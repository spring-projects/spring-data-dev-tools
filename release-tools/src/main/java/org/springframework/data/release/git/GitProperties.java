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
package org.springframework.data.release.git;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import javax.annotation.PostConstruct;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.release.utils.HttpBasicCredentials;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Configurable properties for Git.
 * 
 * @author Oliver Gierke
 */
@Data
@Component
@ConfigurationProperties(prefix = "git")
public class GitProperties {

	private @Getter(AccessLevel.PRIVATE) String username, password;
	private String author, email;

	@PostConstruct
	public void init() {

		Assert.hasText(username, "No GitHub username (git.username) configured!");
		Assert.hasText(username, "No GitHub password (git.password) configured!");
		Assert.hasText(username, "No Git author (git.author) configured!");
		Assert.hasText(username, "No Git email (git.email) configured!");
	}

	/**
	 * Returns the jGit {@link CredentialsProvider} to be used.
	 * 
	 * @return
	 */
	public CredentialsProvider getCredentials() {
		return new UsernamePasswordCredentialsProvider(username, password);
	}

	public HttpBasicCredentials getHttpCredentials() {
		return new HttpBasicCredentials(username, password);
	}
}
