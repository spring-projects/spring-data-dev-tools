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
package org.springframework.data.microbenchmark.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Oliver Drotbohm
 */
interface JpaBookRepository extends CrudRepository<Book, Long> {
	
	static final String BY_TITLE_JPQL = "select b from Book b where b.title = :title";

	@Query(BY_TITLE_JPQL)
	Book findDeclaredByTitle(String title);

	@Transactional(readOnly = true)
	@Query(BY_TITLE_JPQL)
	Book findTransactionalDeclaredByTitle(String title);
	
	Book findDerivedByTitle(String title);

	@Transactional(readOnly = true)
	Book findTransactionalDerivedByTitle(String title);

	Optional<Book> findOptionalDerivedByTitle(String title);

	Book findByIdIs(Long id);

	@Transactional
	Book findReadWriteTransactionBookByIdIs(Long id);

	@Transactional(readOnly = true)
	Book findReadonlyTransactionByIdIs(Long id);

}
