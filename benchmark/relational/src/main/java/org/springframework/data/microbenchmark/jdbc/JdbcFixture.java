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

import lombok.Getter;

import java.lang.reflect.Field;

import org.springframework.aop.framework.Advised;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.repository.support.SimpleJdbcRepository;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.microbenchmark.FixtureUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.ReflectionUtils;

/**
 * Test fixture for JDBC and Spring Data JDBC benchmarks.
 *
 * @author Oliver Drotbohm
 */
class JdbcFixture {

	private final @Getter ConfigurableApplicationContext context;
	private final @Getter RowMapper<Book> bookMapper;

	JdbcFixture(String database) {

		this.context = FixtureUtils.createContext(JdbcApplication.class, "jdbc", database);

		// disableEntityCallbacks(context);

		this.bookMapper = (rs, rowNum) -> new Book(rs.getLong("id"), rs.getString("title"), rs.getInt("pages"));
	}

	private static void disableEntityCallbacks(ApplicationContext context) {

		JdbcBookRepository repository = context.getBean(JdbcBookRepository.class);

		Field field = ReflectionUtils.findField(SimpleJdbcRepository.class, "entityOperations");
		ReflectionUtils.makeAccessible(field);

		try {
			JdbcAggregateTemplate aggregateTemplate = (JdbcAggregateTemplate) ReflectionUtils.getField(field,
					((Advised) repository).getTargetSource().getTarget());

			field = ReflectionUtils.findField(JdbcAggregateTemplate.class, "publisher");
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, aggregateTemplate, NoOpApplicationEventPublisher.INSTANCE);

			aggregateTemplate.setEntityCallbacks(NoOpEntityCallbacks.INSTANCE);

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}
	}

	@SpringBootApplication
	static class JdbcApplication {}

	enum NoOpApplicationEventPublisher implements ApplicationEventPublisher {

		INSTANCE;

		@Override
		public void publishEvent(Object event) {}
	}

	enum NoOpEntityCallbacks implements EntityCallbacks {

		INSTANCE;

		@Override
		public void addEntityCallback(EntityCallback<?> callback) {}

		@Override
		@SuppressWarnings("rawtypes")
		public <T> T callback(Class<? extends EntityCallback> callbackType, T entity, Object... args) {
			return entity;
		}
	}
}
