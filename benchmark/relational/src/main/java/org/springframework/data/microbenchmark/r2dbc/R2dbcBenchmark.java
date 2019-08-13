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
package org.springframework.data.microbenchmark.r2dbc;

import java.util.function.Function;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.r2dbc.core.DatabaseClient;

import io.r2dbc.spi.Row;

/**
 * Benchmark for R2DBC and Spring Data R2DBC
 * 
 * @author Oliver Drotbohm
 */
public class R2dbcBenchmark extends AbstractMicrobenchmark {
	
	private static final String FIND_ALL_SQL = "SELECT id, title, pages FROM Book";
	private static final String BY_TITLE_SQL = FIND_ALL_SQL + " where title = :title";

	@Param({ /* "postgres", */ "h2-in-memory" /*, "h2" */ })
	String profile;
	
	private DatabaseClient operations;
	private Function<Row, Book> mapper;
	
	private R2dbcBookRepository repository;

	@Setup
	public void setUp() {

		R2dbcFixture fixture = new R2dbcFixture(profile);
		
		ConfigurableApplicationContext context = fixture.getContext();
		
		this.operations = context.getBean(DatabaseClient.class);
		this.mapper = row -> new Book(row.get("id", Long.class), row.get("title", String.class), row.get("pages", Integer.class));
		
		this.repository = context.getBean(R2dbcBookRepository.class);
	}

	@Benchmark
	public void findByTitle(Blackhole sink) {
		
		sink.consume(operations.execute(BY_TITLE_SQL) //
				.bind("title", "title0") //
				.map(mapper)
				.one() //
				.block());
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		
		sink.consume(operations.execute(FIND_ALL_SQL) //
				.map(mapper) //
				.all() //
				.collectList() //
				.block());
	}

	@Benchmark
	public void repositoryFindByTitle(Blackhole sink) {
		sink.consume(repository.findByTitle("title0").block());
	}
	
	@Benchmark
	public void repositoryFindTransactionalByTitle(Blackhole sink) {
		sink.consume(repository.findTransactionalByTitle("title0").block());
	}

	@Benchmark
	public void repositoryFindAll(Blackhole sink) {
		sink.consume(repository.findAll().collectList().block());
	}
	
	public static void main(String[] args) {
		
		new R2dbcFixture("postgres") //
			.getContext() //
			.getBean(R2dbcBookRepository.class) //
			.findAll() //
			.collectList() //
			.block();
	}
}
