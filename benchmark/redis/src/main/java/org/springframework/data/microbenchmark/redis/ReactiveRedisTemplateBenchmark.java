/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.microbenchmark.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Benchmarks for {@link ReactiveRedisTemplate}.
 *
 * @author Mark Paluch
 */
public class ReactiveRedisTemplateBenchmark extends AbstractMicrobenchmark {

	private ClientResources clientResources;
	private ReactiveRedisTemplate<String, String> template;
	private RedisClient client;
	private StatefulRedisConnection<String, String> connection;

	@Setup
	public void setUp() {

		clientResources = DefaultClientResources.create();

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		connectionFactory.setClientResources(clientResources);
		connectionFactory.afterPropertiesSet();

		ReactiveRedisConnection reactiveConnection = connectionFactory.getReactiveConnection();
		reactiveConnection.keyCommands().del(ByteBuffer.wrap("user".getBytes())).block();
		reactiveConnection.close();

		client = RedisClient.create(clientResources, RedisURI.create("localhost", 6379));
		connection = client.connect();
		template = new ReactiveRedisTemplate<>(connectionFactory, RedisSerializationContext.string());
	}

	@TearDown
	public void tearDown() {
		connection.close();
		client.shutdown(Duration.ZERO, Duration.ZERO);
		clientResources.shutdown(0, 0, TimeUnit.MILLISECONDS);
	}

	@Benchmark
	public void clientOnly() {
		connection.sync().incr("user");
	}

	@Benchmark
	@OperationsPerInvocation(1000)
	public long clientReactiveConcatMap() {
		return Flux.range(0, 1000).concatMap(it -> connection.reactive().incr("user")).blockLast();
	}

	@Benchmark
	@OperationsPerInvocation(1000)
	public long clientReactiveFlatMap() {
		return Flux.range(0, 1000).flatMap(it -> connection.reactive().incr("user")).blockLast();
	}

	@Benchmark
	@OperationsPerInvocation(1000)
	public long templateReactiveConcatMap() {
		return Flux.range(0, 1000).concatMap(it -> template.opsForValue().increment("user")).blockLast();
	}

	@Benchmark
	@OperationsPerInvocation(1000)
	public long templateReactiveFlatMap() {
		return Flux.range(0, 1000).flatMap(it -> template.opsForValue().increment("user")).blockLast();
	}

	/*
	@Benchmark
	@OperationsPerInvocation(1000)
	public long templateReactiveExecuteInSessionConcatMap() {
		return template
				.executeInSession(session -> Flux.range(0, 1000).concatMap(i -> session.opsForValue().increment("user")))
				.blockLast();
	}

	@Benchmark
	@OperationsPerInvocation(1000)
	public long templateReactiveExecuteInSessionFlatMap() {
		return template
				.executeInSession(session -> Flux.range(0, 1000).flatMap(i -> session.opsForValue().increment("user")))
				.blockLast();
	} */

}
