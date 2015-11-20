/*
 * Copyright 2015 the original author or authors.
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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

/**
 * Utility method to easily execute functionality in parallel.
 * 
 * @author Oliver Gierke
 */
public class ExecutionUtils {

	/**
	 * Runs the given {@link ConsumerWithException} for each element in the given {@link Iterable} in parallel waiting for
	 * all executions to complete before returning. Exceptions being thrown in the {@link ConsumerWithException} will be
	 * converted into {@link RuntimeException}s.
	 * 
	 * @param iterable must not be {@literal null}.
	 * @param consumer must not be {@literal null}.
	 */
	public static <T> void run(Iterable<T> iterable, ConsumerWithException<T> consumer) {

		Assert.notNull(iterable, "Iterable must not be null!");
		Assert.notNull(consumer, "Consumer must not be null!");

		StreamSupport.stream(iterable.spliterator(), false).//
				map(it -> CompletableFuture.runAsync(() -> {
					try {
						consumer.accept(it);
					} catch (Exception o_O) {
						throw new RuntimeException(o_O);
					}
				})).collect(Collectors.toList()).forEach(future -> future.join());

	}

	/**
	 * Runs the given {@link Function} for each element in the given {@link Iterable} in parallel waiting for all
	 * executions to complete before returning the results.
	 * 
	 * @param iterable must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public static <T, S> Collection<S> runAndReturn(Iterable<T> iterable, Function<T, S> function) {

		Assert.notNull(iterable, "Iterable must not be null!");
		Assert.notNull(function, "Consumer must not be null!");

		return StreamSupport.stream(iterable.spliterator(), false).//
				map(it -> CompletableFuture.supplyAsync(() -> function.apply(it))).//
				collect(Collectors.toList()).//
				stream().//
				map(future -> future.join()).//
				collect(Collectors.toList());
	}

	public static interface ConsumerWithException<T> {

		void accept(T t) throws Exception;
	}
}
