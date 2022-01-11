/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.data.microbenchmark.mongodb;

import lombok.Getter;

import java.util.Collections;
import java.util.stream.IntStream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * @author Oliver Drotbohm
 */
class MongoDbFixture {

	private final @Getter ConfigurableApplicationContext context;

	MongoDbFixture() {

		SpringApplication application = new SpringApplication();
		application.addPrimarySources(Collections.singletonList(MongoDbApplication.class));
		application.setAdditionalProfiles("jpa");
		application.setLazyInitialization(true);

		this.context = application.run();

		MongoOperations operations = context.getBean(MongoOperations.class);

		operations.dropCollection(Book.class);

		IntStream.range(0, Constants.NUMBER_OF_BOOKS) //
			.mapToObj(it -> new Book("title" + it, it)) //
			.forEach(operations::save);
	}

	@SpringBootApplication
	static class MongoDbApplication {}
}
