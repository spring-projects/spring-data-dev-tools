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

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.query.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A repository for {@link Book} instances.
 * 
 * @author Oliver Drotbohm
 */
interface R2dbcBookRepository extends R2dbcRepository<Book, Long> {
	
	static final String BY_TITLE = "SELECT id, title, pages FROM Book where title = :title";
	
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	Flux<Book> findAll();
	
	@Query(BY_TITLE)
	Mono<Book> findByTitle(String title);
	
	@Transactional(readOnly = true)
	@Query(BY_TITLE)
	Mono<Book> findTransactionalByTitle(String title);
}
