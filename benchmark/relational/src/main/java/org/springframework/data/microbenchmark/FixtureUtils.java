/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.microbenchmark;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FixtureUtils {

	public static final int NUMBER_OF_BOOKS = 8;

	public static ConfigurableApplicationContext createContext(Class<?> configuration, String api, String database) {

		SpringApplication application = new SpringApplication();
		application.addPrimarySources(Collections.singletonList(configuration));
		application.setLazyInitialization(true);
		application.setAdditionalProfiles(api, database);

		System.out.println("Activating profiles: " + Arrays.asList(api, database).toString());

		return application.run();
	}
}
