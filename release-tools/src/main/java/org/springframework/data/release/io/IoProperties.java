/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "io")
class IoProperties {

	private File workDir, javaHome, logs;

	public void setWorkDir(String workDir) {

		log.info(String.format("🔧 Using %s as working directory!", workDir));
		this.workDir = new File(workDir.replace("~", System.getProperty("user.home")));
	}

	public void setJavaHome(String javaHome) {

		if (!StringUtils.hasText(javaHome)) {
			return;
		}

		File javaHomeDir = new File(javaHome.replace("~", System.getProperty("user.home")));

		if (!javaHomeDir.isDirectory()) {
			log.warn(String.format(
					"⚠️️ Property 'io.javaHome' does not point to a vaild directory ('%s')! Falling back to os.default.",
					javaHomeDir.getPath()));
			return;
		}

		log.info(String.format("🔧 Setting javaHome to: '%s'.", javaHomeDir.getPath()));
		this.javaHome = javaHomeDir;
	}
}
