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
package org.springframework.data.microbenchmark.jdbc;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.util.Optional;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jdbc.core.convert.EntityRowMapper;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * Benchmark for JDBC and Spring Data JDBC
 * 
 * @author Oliver Drotbohm
 */
public class JdbcBenchmark extends AbstractMicrobenchmark {

	private static final String BY_TITLE_SQL = "SELECT id, title, pages FROM Book where title = ?";

	@Param({ /*"postgres",*/ "h2-in-memory", /*"h2"*/ }) String profile;

	JdbcOperations operations;
	RowMapper<Book> bookMapper;
	EntityRowMapper<Book> bookEntityMapper;
	JdbcBookRepository repository;

	Set<String> columns;
	HashMap<String, Object> values;
	@Setup
	@SuppressWarnings("unchecked")
	public void setUp() {

		JdbcFixture fixture = new JdbcFixture(profile);

		this.bookMapper = fixture.getBookMapper();

		ConfigurableApplicationContext context = fixture.getContext();

		this.operations = context.getBean(JdbcOperations.class);
		this.repository = context.getBean(JdbcBookRepository.class);

		JdbcConverter converter = context.getBean(JdbcConverter.class);
		JdbcMappingContext mappingContext = context.getBean(JdbcMappingContext.class);

		RelationalPersistentEntity<Book> requiredPersistentEntity = (RelationalPersistentEntity<Book>) mappingContext
				.getRequiredPersistentEntity(Book.class);

		this.bookEntityMapper = new EntityRowMapper<Book>(requiredPersistentEntity, converter);

		// ResultSet mock

		this.columns = new TreeSet<>();
		columns.add("id");
		columns.add("title");
		columns.add("pages");

		this.values = new HashMap<>();
		values.put("id", 1L);
		values.put("title", "title0");
		values.put("pages", 42L);
	}

	@Benchmark
	public void convertWithSpringData(Blackhole sink) throws Exception {

		MockResultSet resultSet = new MockResultSet("book");
		resultSet.addColumns(columns);
		resultSet.addRow(values);
		resultSet.next();

		sink.consume(this.bookEntityMapper.mapRow(resultSet, 1));
	}

	@Benchmark
	public void findByTitle(Blackhole sink) {
		sink.consume(operations.queryForObject(BY_TITLE_SQL, new Object[] { "title0" }, bookMapper));
	}

	@Benchmark
	public void findByTitleOptional(Blackhole sink) {
		sink.consume(Optional.of(operations.queryForObject(BY_TITLE_SQL, new Object[] { "title0" }, bookMapper)));
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		sink.consume(operations.query("SELECT id, title, pages FROM Book", bookMapper));
	}

	@Benchmark
	public void findAllWithSpringDataConversion(Blackhole sink) {
		sink.consume(operations.query("SELECT id, title, pages FROM Book", bookEntityMapper));
	}

	@Benchmark
	public void repositoryFindByTitle(Blackhole sink) {
		sink.consume(repository.findByTitle("title0"));
	}

	@Benchmark
	public void repositoryFindTransactionalByTitle(Blackhole sink) {
		sink.consume(repository.findTransactionalByTitle("title0"));
	}

	@Benchmark
	public void repositoryFindByTitleOptional(Blackhole sink) {
		sink.consume(repository.findOptionalByTitle("title0"));
	}

	@Benchmark
	public void repositoryFindAll(Blackhole sink) {
		sink.consume(repository.findAll());
	}
}
