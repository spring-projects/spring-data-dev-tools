/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.release.deployment;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration to set up deployment components.
 * 
 * @author Oliver Gierke
 */
@Configuration
class DeploymentConfiguration {

	@Autowired DeploymentProperties properties;

	@Bean
	public ArtifactoryClient client(Logger logger) {
		return new ArtifactoryClient(artifactoryRestTemplate(), logger, properties);
	}

	@Bean
	public RestTemplate artifactoryRestTemplate() {

		RestTemplate template = new RestTemplate();
		template.setInterceptors(Arrays.asList(new AuthenticatingClientHttpRequestInterceptor(properties)));

		return template;
	}

	@RequiredArgsConstructor
	private static class AuthenticatingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

		private final @NonNull DeploymentProperties properties;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.http.client.ClientHttpRequestInterceptor#intercept(org.springframework.http.HttpRequest, byte[], org.springframework.http.client.ClientHttpRequestExecution)
		 */
		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {

			request.getHeaders().add("X-Api-Key", properties.getApiKey());
			request.getHeaders().add("Authentication", properties.getCredentials().toString());

			return execution.execute(request, body);
		}
	}
}
