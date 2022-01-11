/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.mockito.Mockito.*;

import org.bson.Document;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * @author Roman Puchkovskiy
 */
public class AfterConvertCallbacksBenchmark extends AbstractMicrobenchmark {

	private MongoTemplate templateWithoutContext;
	private MongoTemplate templateWithEmptyContext;
	private MongoTemplate templateWithContext;

	private Person source;

	@Setup
	public void setUp() {

		MongoClient client = mock(MongoClient.class);
		MongoDatabase db = mock(MongoDatabase.class);
		MongoCollection<Document> collection = mock(MongoCollection.class);

		when(client.getDatabase(anyString())).thenReturn(db);
		when(db.getCollection(anyString(), eq(Document.class))).thenReturn(collection);

		MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(client, "mock-database");

		templateWithoutContext = new MongoTemplate(factory);

		templateWithEmptyContext = new MongoTemplate(factory);
		StaticApplicationContext empty = new StaticApplicationContext();
		empty.refresh();
		templateWithEmptyContext.setApplicationContext(empty);

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
	static class EntityCallbackConfig {

		@Bean
		AfterConvertCallback<Person> afterConvertCallback() {
			return new PersonAfterConvertCallback();
		}

		private static class PersonAfterConvertCallback implements AfterConvertCallback<Person> {

			@Override
			public Person onAfterConvert(Person it, Document document, String collection) {

				Person target = new Person();
				target.id = it.id;
				target.firstname = it.firstname = "luke";
				target.lastname = it.lastname = "skywalker";

				target.address = it.address;
				return target;
			}
		}
	}

	static class Person {

		@Id String id;
		String firstname;
		String lastname;
		Address address;
	}

	static class Address {

		String city;
		String street;
	}

}
