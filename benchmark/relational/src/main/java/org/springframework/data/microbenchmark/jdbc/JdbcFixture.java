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
package org.springframework.data.microbenchmark.jdbc;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.Constants;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Oliver Drotbohm
 */
public class JdbcFixture {

	private final @Getter JdbcOperations operations;
	private final @Getter RowMapper<Book> bookMapper;
	private final List<Book> books = new ArrayList<>();

	private final @Getter ConfigurableApplicationContext context;

	public JdbcFixture() {

		SpringApplication application = new SpringApplication();
		application.addPrimarySources(Collections.singletonList(JdbcApplication.class));
		application.setLazyInitialization(true);
		application.setAdditionalProfiles("jdbc");
		this.context = application.run();

		this.operations = context.getBean(JdbcOperations.class);

		// Schema
		this.operations.execute(
				"create table Book (id bigint not null auto_increment, title varchar(255), pages integer not null, primary key (id))");

		// Data
		IntStream.range(0, Constants.NUMBER_OF_BOOKS) //
				.mapToObj(it -> new Book(null, "title" + it, it)) //
				.peek(it -> books.add(it)) //
				.forEach(it -> operations.update("INSERT INTO Book VALUES (null, ?1, ?2)", it.getTitle(), it.getPages()));

		this.bookMapper = (rs, rowNum) -> new Book(rs.getLong("id"), rs.getString("title"), rs.getInt("pages"));
	}

	@SpringBootApplication
	static class JdbcApplication {

	}

	public void close() {
		context.close();
	}
}
