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

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A repository for {@link Book} instances.
 * 
 * @author Oliver Drotbohm
 */
interface JdbcBookRepository extends CrudRepository<Book, Long> {
	
	static final String BY_TITLE = "SELECT id, title, pages FROM Book where title = :title";
	
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	Iterable<Book> findAll();
	
	@Query(BY_TITLE)
	Book findByTitle(String title);
	
	@Transactional(readOnly = true)
	@Query(BY_TITLE)
	Book findTransactionalByTitle(String title);

	@Query(BY_TITLE)
	Optional<Book> findOptionalByTitle(String title);
}
