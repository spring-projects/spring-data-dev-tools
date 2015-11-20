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
package org.springframework.data.release.io;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "io")
public class IoProperties {

	private String workDir;

	public void setWorkDir(String workDir) {

		this.workDir = workDir.replace("~", System.getProperty("user.home"));
		log.info(String.format("Using %s as working directory!", workDir));
	}

	public File getWorkDir() {
		return new File(workDir);
	}
}
