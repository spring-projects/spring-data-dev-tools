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

import java.util.ArrayList;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.mongodb.core.ExecutableFindOperation.ExecutableFind;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.Function;
import com.mongodb.client.MongoCollection;

/**
 * @author Oliver Drotbohm
 */
public class MongoDbBenchmark extends AbstractMicrobenchmark {
	
	private static final Query BY_TITLE = Query.query(Criteria.where("title").is("title0"));

	private MongoCollection<Document> collection;
	private Function<Document, Book> mapper;
	
	private ExecutableFind<Book> findBook;
	private MongoDbBookRepository repository;
	
	private MappingMongoConverter converter;
	private Bson bookSource;

	@Setup
	public void setUp() {
		
		ConfigurableApplicationContext context = new MongoDbFixture().getContext();
		
		MongoOperations operations = context //
				.getBean(MongoOperations.class);
		
		this.collection = operations.getCollection(operations.getCollectionName(Book.class));
		this.mapper = document -> new Book(document.getObjectId("_id"), document.getString("title"), document.getInteger("pages"));
		
		this.findBook = operations //
				.query(Book.class);
		
		this.repository = context //
				.getBean(MongoDbBookRepository.class);
		
		this.converter = context //
				.getBean(MappingMongoConverter.class);
		
		this.bookSource = new Document("title", "title1") //
				.append("pages", 42) //
				.append("_id", ObjectId.get());
	}
	
	@Benchmark
	public void convertSingleBook(Blackhole sink) {
		sink.consume(converter.read(Book.class, bookSource));
	}
	
	@Benchmark
	public void rawFindAll(Blackhole sink) {
		sink.consume(collection.find().map(mapper).into(new ArrayList<>()));
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		sink.consume(findBook.all());
	}
	
	@Benchmark
	public void repositoryFindAll(Blackhole sink) {
		sink.consume(repository.findAll());
	}
	
	@Benchmark
	public void rawFindByTitle(Blackhole sink) {
		
		sink.consume(collection.find() //
				.filter(new Document("title", "title0")) //
				.map(mapper) //
				.first());
	}

	@Benchmark
	public void findByTitle(Blackhole sink) {		
		sink.consume(findBook.matching(BY_TITLE).firstValue());
	}
	
	@Benchmark
	public void repositoryFindByTitle(Blackhole sink) {
		sink.consume(repository.findDerivedByTitle("title0"));
	}

	@Benchmark
	public void findByTitleOptional(Blackhole sink) {
		sink.consume(findBook.matching(BY_TITLE).first());
	}
	
	@Benchmark
	public void repositoryFindByTitleOptional(Blackhole sink) {
		sink.consume(repository.findOptionalDerivedByTitle("title0"));
	}
	

	@Benchmark
	public void repositoryFindByTitleDeclared(Blackhole sink) {
		sink.consume(repository.findDeclaredByTitle("title0"));
	}
}
