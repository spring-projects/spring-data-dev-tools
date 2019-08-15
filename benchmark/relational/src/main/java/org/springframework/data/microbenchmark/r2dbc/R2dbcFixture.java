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

import lombok.Getter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.FixtureUtils;

/**
 * Test fixture for JDBC and Spring Data JDBC benchmarks.
 *
 * @author Oliver Drotbohm
 */
public class R2dbcFixture {

	private final @Getter ConfigurableApplicationContext context;

	public R2dbcFixture(String database) {
		this.context = FixtureUtils.createContext(R2dbcApplication.class, "r2dbc", database);
	}

	@SpringBootApplication
	static class R2dbcApplication {}
}
