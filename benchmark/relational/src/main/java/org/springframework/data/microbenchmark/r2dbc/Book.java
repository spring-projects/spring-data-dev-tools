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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;

/**
 * @author Oliver Drotbohm
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__(@PersistenceConstructor))
class Book {

	@Wither(AccessLevel.PACKAGE) @Id Long id;
	String title;
	int pages;
	
	public Book(String title, int pages) {
		
		this.id = null;
		this.title = title;
		this.pages = pages;
	}
}
