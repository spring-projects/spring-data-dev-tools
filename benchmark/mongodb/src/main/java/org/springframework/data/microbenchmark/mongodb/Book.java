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
package org.springframework.data.microbenchmark.mongodb;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.bson.types.ObjectId;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Oliver Drotbohm
 */
@Value
@Document
@AllArgsConstructor(onConstructor = @__(@PersistenceConstructor))
public class Book {

	@Id ObjectId id;
	String title;
	int pages;

	public Book(String title, int pages) {

		this.id = ObjectId.get();
		this.title = title;
		this.pages = pages;
	}
}
