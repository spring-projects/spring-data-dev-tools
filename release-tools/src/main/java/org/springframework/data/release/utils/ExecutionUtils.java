/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.release.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.data.release.Streamable;
import org.springframework.util.Assert;

/**
 * Utility method to easily execute functionality in parallel.
 *
 * @author Oliver Gierke
 */
@Slf4j
public class ExecutionUtils {

	/**
	 * Runs the given {@link ConsumerWithException} for each element in the given {@link Iterable} in parallel waiting for
	 * all executions to complete before returning. Exceptions being thrown in the {@link ConsumerWithException} will be
	 * converted into {@link RuntimeException}s.
	 *
	 * @param streamable must not be {@literal null}.
	 * @param consumer must not be {@literal null}.
	 */
	public static <T> void run(Streamable<T> streamable, ConsumerWithException<T> consumer) {

		Assert.notNull(streamable, "Streamable must not be null!");
		Assert.notNull(consumer, "Consumer must not be null!");

		streamable.stream().//
				map(it -> CompletableFuture.runAsync(() -> {
					try {
						consumer.accept(it);
					} catch (Exception o_O) {
						log.error(o_O.getMessage(), o_O);
						throw new RuntimeException(o_O);
					}
				})).collect(Collectors.toList()).forEach(CompletableFuture::join);
	}

	/**
	 * Runs the given {@link Function} for each element in the given {@link Streamable} in parallel waiting for all
	 * executions to complete before returning the results.
	 *
	 * @param streamable must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public static <T, S> Collection<S> runAndReturn(Streamable<T> streamable, Function<T, S> function) {
		return runAndReturn(streamable, function, Collectors.toList());
	}

	public static <T, S, R> R runAndReturn(Streamable<T> streamable, Function<T, S> function,
			Collector<? super S, ?, R> collector) {

		Assert.notNull(streamable, "Iterable must not be null!");
		Assert.notNull(function, "Consumer must not be null!");

		return streamable.stream().//
				map(it -> CompletableFuture.supplyAsync(() -> function.apply(it))).//
				filter(Objects::nonNull).//
				collect(Collectors.toList()).//
				stream().//
				map(CompletableFuture::join).//
				collect(collector);
	}

	public static interface ConsumerWithException<T> {

		void accept(T t) throws Exception;
	}
}
