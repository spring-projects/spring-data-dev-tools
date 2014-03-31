/*
 * Copyright 2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.EqualsAndHashCode;

import org.springframework.util.Assert;

/**
 * Value object to represent a collection of {@link Tag}s.
 * 
 * @author Oliver Gierke
 */
@EqualsAndHashCode
public class Tags implements Iterable<Tag> {

	private final List<Tag> tags;

	/**
	 * Creates a new {@link Tags} instance for the given {@link List} of {@link Tag}s.
	 * 
	 * @param source must not be {@literal null}.
	 */
	Tags(List<Tag> source) {

		Assert.notNull(source, "Tags must not be null!");

		List<Tag> tags = new ArrayList<>(source);
		Collections.sort(tags);

		this.tags = Collections.unmodifiableList(tags);
	}

	/**
	 * Returns the latest {@link Tag}.
	 * 
	 * @return
	 */
	public Tag getLatest() {
		return tags.get(0);
	}

	/**
	 * Returns all {@link Tag}s as {@link List}.
	 * 
	 * @return
	 */
	public List<Tag> asList() {
		return tags;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Tag> iterator() {
		return tags.iterator();
	}
}
