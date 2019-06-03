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
package org.springframework.data.microbenchmark.mongodb;

import org.bson.Document;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.microbenchmark.mongodb.ProjectionsBenchmark.Address;
import org.springframework.data.microbenchmark.mongodb.ProjectionsBenchmark.Person;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * @author Christoph Strobl
 */
public class CallbacksBenchmark extends AbstractMicrobenchmark {

	private MongoTemplate templateWithoutContext;
	private MongoTemplate templateWithEmptyContext;
	private MongoTemplate templateWithContext;

	private Person source;

	@Setup
	public void setUp() {

		MongoClient client = Mockito.mock(MongoClient.class);
		MongoDatabase db = Mockito.mock(MongoDatabase.class);
		MongoCollection<Document> collection = Mockito.mock(MongoCollection.class);

		Mockito.when(client.getDatabase(Mockito.anyString())).thenReturn(db);
		Mockito.when(db.getCollection(Mockito.anyString(), Mockito.eq(Document.class))).thenReturn(collection);

		MongoDbFactory factory = new SimpleMongoClientDbFactory(client, "mock-database");

		templateWithoutContext = new MongoTemplate(factory);

		templateWithEmptyContext = new MongoTemplate(factory);
		templateWithEmptyContext.setApplicationContext(new AnnotationConfigApplicationContext(EmptyConfig.class));

		templateWithContext = new MongoTemplate(factory);
		templateWithContext.setApplicationContext(new AnnotationConfigApplicationContext(EntityCallbackConfig.class));

		source = new Person();
		source.id = "luke-skywalker";
		source.firstname = "luke";
		source.lastname = "skywalker";

		source.address = new Address();
		source.address.street = "melenium falcon 1";
		source.address.city = "deathstar";
	}

	@Benchmark
	public Object baseline() {
		return templateWithoutContext.save(source);
	}

	@Benchmark
	public Object emptyContext() {
		return templateWithEmptyContext.save(source);
	}

	@Benchmark
	public Object entityCallbacks() {
		return templateWithContext.save(source);
	}

	@Configuration
	static class EmptyConfig {

	}

	@Configuration
	static class EntityCallbackConfig {

		@Bean
		BeforeConvertCallback<Person> convertCallback() {
			return (it, Document) -> {

				Person target = new Person();
				target.id = it.id;
				target.firstname = it.firstname = "luke";
				target.lastname = it.lastname = "skywalker";

				target.address = it.address;
				return target;
			};
		}
	}
}
