/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.release.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.springframework.util.Assert;

/**
 * Base class to easily create a {@link Collector} to create a collection wrapper type.
 * 
 * @author Oliver Gierke
 */
public abstract class ListWrapperCollector<T, S> implements Collector<T, List<T>, S> {

	/**
	 * Creates a new {@link Collector} to eventually apply the given function to turn the {@link List} of elements into a
	 * wrapper type.
	 * 
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public static <T, S> ListWrapperCollector<T, S> collectInto(Function<List<T>, S> function) {

		Assert.notNull(function, "Finisher function must not be null!");

		return new ListWrapperCollector<T, S>() {

			/* 
			 * (non-Javadoc)
			 * @see java.util.stream.Collector#finisher()
			 */
			@Override
			public Function<List<T>, S> finisher() {
				return function;
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.stream.Collector#supplier()
	 */
	@Override
	public Supplier<List<T>> supplier() {
		return ArrayList::new;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.stream.Collector#accumulator()
	 */
	@Override
	public BiConsumer<List<T>, T> accumulator() {
		return List::add;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.stream.Collector#combiner()
	 */
	@Override
	public BinaryOperator<List<T>> combiner() {
		return (left, right) -> {
			left.addAll(right);
			return left;
		};
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.stream.Collector#characteristics()
	 */
	@Override
	public Set<Characteristics> characteristics() {
		return Collections.emptySet();
	}
}
