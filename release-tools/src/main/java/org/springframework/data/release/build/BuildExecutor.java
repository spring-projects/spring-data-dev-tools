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
package org.springframework.data.release.build;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.springframework.data.release.infra.InfrastructureOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.JavaVersion;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.ProjectAware;
import org.springframework.data.release.utils.ListWrapperCollector;
import org.springframework.data.util.Streamable;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Build executor service.
 *
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor
class BuildExecutor {

	private final @NonNull PluginRegistry<BuildSystem, Project> buildSystems;
	private final MavenProperties mavenProperties;
	private final ExecutorService executor;
	private final Workspace workspace;

	@PreDestroy
	public void shutdown() {
		executor.shutdown();
	}

	/**
	 * Selects the build system for each module contained in the given iteration and executes the given function for it
	 * considering pre-requites, honoring the order.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public <T, M extends ProjectAware> Summary<T> doWithBuildSystemOrdered(Streamable<M> iteration,
			BiFunction<BuildSystem, M, T> function) {
		return doWithBuildSystem(iteration, function, true);
	}

	/**
	 * Selects the build system for each module contained in the given iteration and executes the given function for it
	 * considering pre-requites, without considering the execution order.
	 *
	 * @param iteration must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public <T, M extends ProjectAware> Summary<T> doWithBuildSystemAnyOrder(Streamable<M> iteration,
			BiFunction<BuildSystem, M, T> function) {
		return doWithBuildSystem(iteration, function, false);
	}

	private <T, M extends ProjectAware> Summary<T> doWithBuildSystem(Streamable<M> iteration,
			BiFunction<BuildSystem, M, T> function, boolean considerDependencyOrder) {

		Map<Project, CompletableFuture<T>> results = new ConcurrentHashMap<>();

		// Add here projects that should be skipped because of a partial deployment to e.g. Sonatype.
		Set<Project> skip = new HashSet<>(Arrays.asList());

		skip.forEach(it -> results.put(it, CompletableFuture.completedFuture(null)));

		for (M moduleIteration : iteration) {

			if (skip.contains(moduleIteration.getProject())) {
				continue;
			}
		}

		for (M moduleIteration : iteration) {

			if (skip.contains(moduleIteration.getProject())) {
				continue;
			}

			if (considerDependencyOrder) {
				Set<Project> dependencies = moduleIteration.getProject().getDependencies();
				for (Project dependency : dependencies) {

					CompletableFuture<T> futureResult = results.get(dependency);

					if (futureResult == null) {

						if (!iteration.stream().map(ProjectAware::getProject).anyMatch(project -> project.equals(dependency))) {
							throw new IllegalStateException(moduleIteration.getProject().getName() + " requires "
									+ dependency.getName() + " which is not part of the Iteration. Please fix Projects/Iterations setup");
						}

						throw new IllegalStateException("No future result for " + dependency.getName() + ", required by "
								+ moduleIteration.getProject().getName());
					}

					futureResult.join();
				}
			}

			CompletableFuture<T> result = run(moduleIteration, function);

			results.put(moduleIteration.getProject(), result);
		}

		return iteration.stream()//
				.map(module -> {

					CompletableFuture<T> future = results.get(module.getProject());

					try {
						return new ExecutionResult<T>(module.getProject(), future.get());
					}

				catch (InterruptedException | ExecutionException e) {
						return new ExecutionResult<T>(module.getProject(), e.getCause());
					}

				}) //
				.collect(toSummaryCollector());
	}

	private <T, M extends ProjectAware> CompletableFuture<T> run(M module, BiFunction<BuildSystem, M, T> function) {

		Assert.notNull(module, "Module must not be null!");

		CompletableFuture<T> result = new CompletableFuture<>();
		Supplier<IllegalStateException> exception = () -> new IllegalStateException(
				String.format("No build system plugin found for project %s!", module.getProject()));

		BuildSystem buildSystem = buildSystems.getPluginFor(module.getProject(), exception)
				.withJavaVersion(detectJavaVersion(module.getProject()));

		Runnable runnable = () -> {

			try {

				result.complete(function.apply(buildSystem, module));
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		};

		executor.execute(runnable);

		return result;
	}

	@SneakyThrows
	public JavaVersion detectJavaVersion(Project project) {

		File ciProperties = workspace.getFile(InfrastructureOperations.CI_PROPERTIES, project);

		if (!ciProperties.exists()) {
			throw new IllegalStateException(String.format("Cannot find %s for project %s", ciProperties, project));
		}

		Properties properties = new Properties();

		try (FileInputStream fis = new FileInputStream(ciProperties)) {
			properties.load(fis);
		}

		return JavaVersion.fromDockerTag(properties.getProperty("java.main.tag"));
	}

	/**
	 * Returns a new collector to toSummaryCollector {@link ExecutionResult} as {@link Summary} using the {@link Stream}
	 * API.
	 *
	 * @return
	 */
	public static <T> Collector<ExecutionResult<T>, ?, Summary<T>> toSummaryCollector() {
		return ListWrapperCollector.collectInto(Summary::new);
	}

	public static class ExecutionResult<T> {

		private final Project project;
		private final T result;
		private final Throwable failure;

		public ExecutionResult(Project project, Throwable failure) {
			this.project = project;
			this.result = null;
			this.failure = failure;
		}

		public ExecutionResult(Project project, T result) {
			this.project = project;
			this.result = result;
			this.failure = null;
		}

		public T getResult() {
			return result;
		}

		@Override
		public String toString() {
			return String.format("%-14s - %s", project.getName(),
					isSuccessful() ? "🆗 Successful" : "🧨 Error: " + failure.getMessage());
		}

		public boolean isSuccessful() {
			return this.failure == null;
		}
	}

	public static class Summary<T> {

		private final List<ExecutionResult<T>> executions;

		public Summary(List<ExecutionResult<T>> executions) {
			this.executions = executions;

			if (!isSuccessful()) {
				throw new BuildFailed(this);
			}
		}

		public List<ExecutionResult<T>> getExecutions() {
			return executions;
		}

		public boolean isSuccessful() {
			return this.executions.stream().allMatch(ExecutionResult::isSuccessful);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append("Execution summary");
			builder.append(IOUtils.LINE_SEPARATOR);
			builder.append(executions.stream().map(it -> "\t" + it).collect(Collectors.joining(IOUtils.LINE_SEPARATOR)));

			return builder.toString();
		}
	}

	static class BuildFailed extends RuntimeException {

		public BuildFailed(Summary<?> summary) {
			super(summary.toString());
		}
	}

}
