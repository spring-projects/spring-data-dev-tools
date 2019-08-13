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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;

/**
 * Benchmarks for JPA and Spring Data JPA.
 * 
 * @author Oliver Drotbohm
 */
public class JpaBenchmark extends AbstractMicrobenchmark {

	@Param({ "postgres", "h2-in-memory", "h2" })
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
	public void findByTitle(Blackhole sink) {

		Query query = em.createQuery("select b from Book b where b.title = ?1");
		query.setParameter(1, "title0");

		sink.consume(query.getSingleResult());
	}

	@Benchmark
	public void findByTitleCriteria(Blackhole sink) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Book> q = cb.createQuery(Book.class);
		Root<Book> c = q.from(Book.class);

		ParameterExpression<String> parameter = cb.parameter(String.class);

		TypedQuery<Book> query = em.createQuery(q.select(c).where(cb.equal(c.get("title"), parameter)));
		query.setParameter(parameter, "title0");

		sink.consume(query.getSingleResult());
	}

	@Benchmark
	public void findByTitleOptional(Blackhole sink) {

		Query query = em.createQuery("select b from Book b where b.title = ?1");
		query.setParameter(1, "title0");

		sink.consume(Optional.of(query.getSingleResult()));
	}

	@Benchmark
	public void findAll(Blackhole sink) {
		sink.consume(em.createQuery("select b from Book b").getResultList());
	}

	@Benchmark
	public void repositoryFindByTitle(Blackhole sink) {
		sink.consume(repository.findDerivedByTitle("title0"));
	}
	
	@Benchmark
	public void repositoryFindTransactionalByTitle(Blackhole sink) {
		sink.consume(repository.findTransactionalDerivedByTitle("title0"));
	}

	@Benchmark
	public void repositoryFindByTitleDeclared(Blackhole sink) {
		sink.consume(repository.findDeclaredByTitle("title0"));
	}
	
	@Benchmark
	public void repositoryFindTransactionalByTitleDeclared(Blackhole sink) {
		sink.consume(repository.findTransactionalDeclaredByTitle("title0"));
	}

	@Benchmark
	public void repositoryFindByTitleOptional(Blackhole sink) {
		sink.consume(repository.findOptionalDerivedByTitle("title0"));
	}

	@Benchmark
	public void repositoryFindAll(Blackhole sink) {
		sink.consume(repository.findAll());
	}
}
