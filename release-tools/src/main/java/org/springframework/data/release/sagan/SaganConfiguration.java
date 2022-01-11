/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.data.release.sagan;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.git.GitProperties;
import org.springframework.data.release.utils.Logger;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the Sagan interaction subsystem.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
class SaganConfiguration {

	@Autowired GitProperties gitProperties;
	@Autowired SaganProperties properties;
	@Autowired Logger logger;

	@Bean
	public SaganOperations saganOperations(GitOperations operations, SaganClient saganClient, Executor executor) {
		return new SaganOperations(operations, executor, saganClient, logger);
	}

	@Bean
	SaganClient saganClient() {

		RestTemplate restTemplate = new RestTemplateBuilder()
				.basicAuthentication(gitProperties.getUsername(), properties.key).build();

		return new DefaultSaganClient(restTemplate, properties, logger);
	}
}
