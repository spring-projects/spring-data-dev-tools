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
package org.springframework.data.release.build;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Maven configuration properties.
 * 
 * @author Oliver Gierke
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "maven")
class MavenProperties {

	private File mavenHome;
	private File localRepository;
	private Map<String, String> plugins;

	/**
	 * Configures the local Maven repository location to use. In case the given folder does not already exists it's
	 * created.
	 * 
	 * @param localRepository must not be {@literal null} or empty.
	 */
	public void setLocalRepository(String localRepository) {

		Assert.hasText(localRepository, "Local repository must not be null!");

		log.info("Using {} as local Maven repository!", localRepository);

		this.localRepository = new File(localRepository.replace("~", System.getProperty("user.home")));

		if (!this.localRepository.exists()) {
			this.localRepository.mkdirs();
		}
	}

	/**
	 * Returns the fully-qualified plugin goal for the given local one.
	 * 
	 * @param goal must not be {@literal null} or empty.
	 * @return
	 */
	public String getFullyQualifiedPlugin(String goal) {

		Assert.hasText(goal, "Goal must not be null or empty!");
		return plugins.containsKey(goal) ? plugins.get(goal) : goal;
	}
}
