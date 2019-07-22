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
package org.springframework.data.microbenchmark.jdbc.springdata;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.microbenchmark.jdbc.Book;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Oliver Drotbohm
 */
public interface JdbcBookRepository extends CrudRepository<Book, Long> {

	@Query("SELECT id, title, pages FROM Book where title = :title")
	Book findByTitle(String title);

	@Query("SELECT id, title, pages FROM Book where title = :title")
	Optional<Book> findOptionalByTitle(String title);
}
