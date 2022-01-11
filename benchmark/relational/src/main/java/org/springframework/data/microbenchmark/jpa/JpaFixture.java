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
package org.springframework.data.microbenchmark.jpa;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.FixtureUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Test fixture for JPA and Spring Data JPA benchmarks.
 * 
 * @author Oliver Drotbohm
 */
class JpaFixture {

	private final @Getter ConfigurableApplicationContext context;

	JpaFixture(String database) {

		this.context = FixtureUtils.createContext(JpaApplication.class, "jpa", database);
		
		withTransactionalEntityManager(em -> {

			IntStream.range(0, FixtureUtils.NUMBER_OF_BOOKS) //
					.mapToObj(it -> new Book(null, "title" + it, it)) //
					.forEach(em::persist);
		});
	}
	
	private void withTransactionalEntityManager(Consumer<EntityManager> consumer) {

		PlatformTransactionManager manager = context.getBean(PlatformTransactionManager.class);
		TransactionStatus status = manager.getTransaction(new DefaultTransactionDefinition());

		EntityManager em = context.getBean(EntityManager.class);

		consumer.accept(em);

		em.flush();
		manager.commit(status);
		em.close();
	}

	@SpringBootApplication
	static class JpaApplication {}
}
