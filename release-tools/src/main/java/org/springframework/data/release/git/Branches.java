/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.git;

import lombok.EqualsAndHashCode;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * Value object to represent a collection of {@link Branch}es.
 * 
 * @author Mark Paluch
 */
@EqualsAndHashCode
public class Branches implements Iterable<Branch> {

	private final List<Branch> branches;

	/**
	 * Creates a new {@link Branches} instance for the given {@link List} of {@link Branch}es.
	 *
	 * @param source must not be {@literal null}.
	 */
	Branches(List<Branch> source) {

		Assert.notNull(source, "Tags must not be null!");

		this.branches = source.stream().//
				sorted().collect(Collectors.toList());
	}

	/**
	 * Returns all {@link Branch}es as {@link List}.
	 * 
	 * @return
	 */
	public List<Branch> asList() {
		return branches;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Branch> iterator() {
		return branches.iterator();
	}
}
