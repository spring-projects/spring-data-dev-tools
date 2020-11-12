/*
 * Copyright 2020 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Support class for GitHub operations.
 *
 * @author Mark Paluch
 */
@RequiredArgsConstructor
class GitHubSupport {

	final RestOperations operations;

	static RestOperations createOperations(RestTemplateBuilder templateBuilder, GitHubProperties properties) {
		return templateBuilder.uriTemplateHandler(new DefaultUriBuilderFactory(properties.getApiUrl())).build();
	}

	/**
	 * Apply a {@link Predicate callback} with GitHub paging starting at {@code endpointUri}. The given
	 * {@link Predicate#test(Object)} outcome controls whether paging continues by returning {@literal true} or stops.
	 *
	 * @param endpointUri
	 * @param method
	 * @param parameters
	 * @param entity
	 * @param type
	 * @param callbackContinue
	 * @param <T>
	 */
	<T> void doWithPaging(String endpointUri, HttpMethod method, Map<String, Object> parameters, HttpEntity<?> entity,
			ParameterizedTypeReference<T> type, Predicate<T> callbackContinue) {

		ResponseEntity<T> exchange = operations.exchange(endpointUri, method, entity, type, parameters);

		Pattern pattern = Pattern.compile("<([^ ]*)>; rel=\"(\\w+)\"");

		while (true) {

			if (!callbackContinue.test(exchange.getBody())) {
				return;
			}

			HttpHeaders responseHeaders = exchange.getHeaders();
			List<String> links = responseHeaders.getValuesAsList("Link");

			if (links.isEmpty()) {
				return;
			}

			String nextLink = null;
			for (String link : links) {

				Matcher matcher = pattern.matcher(link);
				if (matcher.find()) {
					if (matcher.group(2).equals("next")) {
						nextLink = matcher.group(1);
						break;
					}
				}
			}

			if (nextLink == null) {
				return;
			}

			exchange = operations.exchange(nextLink, method, entity, type, parameters);
		}
	}

}
