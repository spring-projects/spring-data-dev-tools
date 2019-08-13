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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.microbenchmark.FixtureUtils;
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator;

import io.r2dbc.spi.ConnectionFactory;
import lombok.Getter;

/**
 * Test fixture for JDBC and Spring Data JDBC benchmarks.
 * 
 * @author Oliver Drotbohm
 */
public class R2dbcFixture {

	private final @Getter ConfigurableApplicationContext context;

	public R2dbcFixture(String database) {
		
		this.context = FixtureUtils.createContext(R2dbcApplication.class, "r2dbc", database);
		
		R2dbcProperties properties = context.getBean(R2dbcProperties.class);
		String platform = properties.getPlatform();
				
		Resource schema = new ClassPathResource(String.format("schema-%s.sql", platform));
		Resource data = new ClassPathResource(String.format("data-%s.sql", platform));
		
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema, data);
		populator.execute(context.getBean(ConnectionFactory.class)).block();
	}
	

	@SpringBootApplication (exclude = {
			DataSourceAutoConfiguration.class
	})
	static class R2dbcApplication {}
}
