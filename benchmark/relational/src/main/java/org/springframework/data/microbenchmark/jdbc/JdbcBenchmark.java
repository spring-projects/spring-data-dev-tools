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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.util.Optional;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;

/**
 * @author Oliver Drotbohm
 */
public class JdbcBenchmark extends AbstractMicrobenchmark {

	private JdbcFixture fixture;

	@Setup
	public void setUp() {
		this.fixture = new JdbcFixture();
	}

	@TearDown
	public void tearDown() {
		fixture.close();
	}

	@Benchmark
	public void findByTitle(Blackhole sink) {

		Book book = fixture.getOperations().queryForObject("SELECT id, title, pages FROM Book where title = ?1",
				new Object[] { "title0" }, fixture.getBookMapper());

		sink.consume(book);
	}

	@Benchmark
	public void findByTitleOptional(Blackhole sink) {

		Book book = fixture.getOperations().queryForObject("SELECT id, title, pages FROM Book where title = ?1",
				new Object[] { "title0" }, fixture.getBookMapper());

		sink.consume(Optional.of(book));
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		sink.consume(fixture.getOperations().query("SELECT id, title, pages FROM Book", fixture.getBookMapper()));
	}
}
