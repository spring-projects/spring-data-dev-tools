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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;

/**
 * Benchmarks for JPA and Spring Data JPA.
 * 
 * @author Oliver Drotbohm
 */
public class JpaBenchmark extends AbstractMicrobenchmark {

	@Param({  "postgres", "mysql", "h2-in-memory" })
	String profile;
	
	EntityManager em;
	JpaBookRepository repository;

	@Setup
	public void setUp() {

		ConfigurableApplicationContext context = new JpaFixture(profile).getContext();

		this.em = context.getBean(EntityManager.class);
		this.repository = context.getBean(JpaBookRepository.class);
	}

	@Benchmark
	public void entityManagerfindById(Blackhole sink) {

		Query query = em.createQuery("select b from Book b where b.id = ?1");
		query.setParameter(1, 1L);

		sink.consume(query.getSingleResult());
	}

	@Benchmark
	public void repositoryFindById(Blackhole sink) {
		sink.consume(repository.findById(1L));
	}

	@Benchmark
	public void derivedFindByIdWithoutTransaction(Blackhole sink) {
		sink.consume(repository.findByIdIs(1L));
	}

	@Benchmark
	public void derivedFindByIdWithTransaction(Blackhole sink) {
		sink.consume(repository.findReadWriteTransactionBookByIdIs(1L));
	}

	@Benchmark
	public void derivedFindByIdWithReadonlyTransaction(Blackhole sink) {
		sink.consume(repository.findReadonlyTransactionByIdIs(1L));
	}
}
