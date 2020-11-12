/*
 * Copyright 2020 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

/**
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
class ExecutorConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "maven", name = "parallelize")
	public ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean() {

		int processors = Runtime.getRuntime().availableProcessors();
		int threadCount = Math.max(2, processors - 4);
		log.info(String.format("Setting up Executor Service with %d Threads", threadCount));

		ThreadPoolExecutorFactoryBean scheduler = new ThreadPoolExecutorFactoryBean();
		scheduler.setCorePoolSize(threadCount);
		scheduler.setQueueCapacity(32);

		return scheduler;
	}

	@Bean
	@ConditionalOnProperty(prefix = "maven", name = "parallelize", matchIfMissing = true, havingValue = "false")
	public ExecutorService executorService() {
		return ImmediateExecutorService.INSTANCE;
	}

	enum ImmediateExecutorService implements ExecutorService {
		INSTANCE;

		@Override
		public void shutdown() {

		}

		@Override
		public List<Runnable> shutdownNow() {
			return Collections.emptyList();
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) {
			return false;
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			try {
				return CompletableFuture.completedFuture(task.call());
			} catch (Exception e) {
				CompletableFuture<T> f = new CompletableFuture<>();
				f.completeExceptionally(e);
				return f;
			}
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			return submit(() -> {
				task.run();
				return result;
			});
		}

		@Override
		public Future<?> submit(Runnable task) {
			return submit(task, null);
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void execute(Runnable command) {
			submit(command);
		}
	}
}
