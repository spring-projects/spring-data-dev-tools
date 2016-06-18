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
package org.springframework.data.release.issues.github;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.release.git.GitProperties;
import org.springframework.data.release.utils.HttpBasicCredentials;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@Data
@Component
@ConfigurationProperties(prefix = "github")
@RequiredArgsConstructor
public class GitHubProperties {

	private final @Getter(AccessLevel.NONE) GitProperties gitProperties;

	private String apiUrl;

	public String getUsername() {
		return gitProperties.getUsername();
	}

	public HttpBasicCredentials getHttpCredentials() {
		return gitProperties.getHttpCredentials();
	}

	@PostConstruct
	public void init() {
		Assert.hasText(apiUrl, "No GitHub API base url configured!");
	}
}
