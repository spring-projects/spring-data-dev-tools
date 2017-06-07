/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.release.issues;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.release.model.Project;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Configuration
@EnableCaching(proxyTargetClass = true)
class IssueTrackerConfiguration {

	@Bean
	CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

	@Bean
	ObjectMapper jacksonObjectMapper() {

		ObjectMapper mapper = new ObjectMapper();

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.registerModule(new ParameterNamesModule(Mode.PROPERTIES));
		mapper.registerModule(new SyntheticLambdaFactoryMethodIgnoringModule());

		return mapper;
	}

	@Bean
	@Qualifier("tracker")
	RestTemplate restTemplate() {

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(jacksonObjectMapper());

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(converter);

		RestTemplate template = new RestTemplate();
		template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
		template.setMessageConverters(converters);

		return template;
	}

	@Bean
	PluginRegistry<IssueTracker, Project> issueTrackers(List<? extends IssueTracker> plugins) {
		return OrderAwarePluginRegistry.create(plugins);
	}

	/**
	 * @author Oliver Gierke
	 */
	static class SyntheticLambdaFactoryMethodIgnoringModule extends SimpleModule {

		private static final long serialVersionUID = -3075786389813846512L;

		@Override
		public void setupModule(SetupContext context) {

			context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {

				private static final long serialVersionUID = 479313244908256455L;

				@Override
				public boolean hasIgnoreMarker(AnnotatedMember m) {

					if (!(m instanceof AnnotatedMethod)) {
						return super.hasIgnoreMarker(m);
					}

					AnnotatedMethod method = (AnnotatedMethod) m;

					return method.getName().startsWith("lambda$") ? true : super.hasIgnoreMarker(m);
				}
			});
		}
	}
}
