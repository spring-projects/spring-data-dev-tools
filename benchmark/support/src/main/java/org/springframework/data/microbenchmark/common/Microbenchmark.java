/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.microbenchmark.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.NoBenchmarksException;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;
import org.springframework.util.StringUtils;

/**
 * JMH Microbenchmark runner that turns methods annotated with {@link Benchmark} into runnable methods allowing
 * execution through JUnit.
 *
 * @author Mark Paluch
 */
public class Microbenchmark extends BlockJUnit4ClassRunner {

	private final Object childrenLock = new Object();
	private final JmhSupport jmhRunner = new JmhSupport();

	private Collection<FrameworkMethod> filteredChildren;

	/**
	 * Creates a {@link Microbenchmark} to run {@link Class test class}.
	 *
	 * @param testClass
	 * @throws InitializationError if the test class is malformed.
	 */
	public Microbenchmark(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	/**
	 * Ignore JUnit validation as we're using JMH here.
	 *
	 * @param errors
	 */
	@Override
	protected void collectInitializationErrors(List<Throwable> errors) {}

	/**
	 * JMH has no means to exclude benchmark methods.
	 */
	@Override
	protected boolean isIgnored(FrameworkMethod child) {
		return false;
	}

	/**
	 * Returns the methods that run tests. Default implementation returns all methods annotated with {@code @Test} on this
	 * class and superclasses that are not overridden.
	 */
	protected List<FrameworkMethod> computeTestMethods() {

		List<FrameworkMethod> annotatedMethods = new ArrayList<>(getTestClass().getAnnotatedMethods(Benchmark.class));

		annotatedMethods.sort(Comparator.comparing(Microbenchmark::getBenchmarkName));

		return Collections.unmodifiableList(annotatedMethods);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runners.ParentRunner#classBlock(org.junit.runner.notification.RunNotifier)
	 */
	@Override
	protected Statement classBlock(RunNotifier notifier) {
		return childrenInvoker(notifier);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runners.ParentRunner#getDescription()
	 */
	@Override
	public Description getDescription() {

		Description description = Description.createSuiteDescription(getName(), getRunnerAnnotations());

		getFilteredChildren().stream().map(this::describeChild).forEach(description::addChild);

		return description;
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runners.ParentRunner#filter(org.junit.runner.manipulation.Filter)
	 */
	public void filter(Filter filter) throws NoTestsRemainException {

		synchronized (childrenLock) {

			List<FrameworkMethod> children = new ArrayList<>(getFilteredChildren());
			List<FrameworkMethod> filtered = children.stream().filter(it -> {

				if (filter.shouldRun(describeChild(it))) {
					try {
						filter.apply(it);
						return true;
					} catch (NoTestsRemainException e) {
						return false;
					}
				}
				return false;
			}).collect(Collectors.toList());

			if (filtered.isEmpty()) {
				throw new NoTestsRemainException();
			}

			filteredChildren = Collections.unmodifiableCollection(filtered);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.runners.ParentRunner#sort(org.junit.runner.manipulation.Sorter)
	 */
	public void sort(Sorter sorter) {

		synchronized (childrenLock) {

			getFilteredChildren().forEach(sorter::apply);

			List<FrameworkMethod> sortedChildren = new ArrayList<>(getFilteredChildren());

			sortedChildren.sort((o1, o2) -> sorter.compare(describeChild(o1), describeChild(o2)));

			filteredChildren = Collections.unmodifiableCollection(sortedChildren);
		}
	}

	/**
	 * Run matching {@link org.openjdk.jmh.annotations.Benchmark} methods with options collected from
	 * {@link org.springframework.core.env.Environment}.
	 */
	@Override
	protected Statement childrenInvoker(RunNotifier notifier) {

		Collection<FrameworkMethod> methods = getFilteredChildren();
		CacheFunction cache = new CacheFunction(methods, this::describeChild);

		if (methods.isEmpty()) {
			return new Statement() {
				@Override
				public void evaluate() {}
			};
		}

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try {
					doRun(notifier, methods, cache);
				} catch (NoBenchmarksException | NoTestsRemainException e) {
					methods.forEach(it -> notifier.fireTestIgnored(describeChild(it)));
				}
			}
		};
	}

	void doRun(RunNotifier notifier, Collection<FrameworkMethod> methods, CacheFunction cache) throws Exception {

		List<String> includes = jmhRunner.includes(getTestClass().getJavaClass(), methods);

		if (includes.isEmpty()) {
			throw new NoTestsRemainException();
		}

		ChainedOptionsBuilder optionsBuilder = jmhRunner.options();

		includes.forEach(optionsBuilder::include);

		Options options = optionsBuilder.build();
		NotifyingOutputFormat notifyingOutputFormat = new NotifyingOutputFormat(notifier, cache,
				createOutputFormat(options));

		jmhRunner.publishResults(new Runner(options, notifyingOutputFormat).run());
	}

	private Collection<FrameworkMethod> getFilteredChildren() {

		if (filteredChildren == null) {
			synchronized (childrenLock) {
				if (filteredChildren == null) {
					filteredChildren = Collections.unmodifiableCollection(getChildren());
				}
			}
		}
		return filteredChildren;
	}

	private static OutputFormat createOutputFormat(Options options) {

		// sadly required here as the check cannot be made before calling this method in constructor
		if (options == null) {
			throw new IllegalArgumentException("Options not allowed to be null.");
		}

		PrintStream out;
		if (options.getOutput().hasValue()) {
			try {
				out = new PrintStream(options.getOutput().get());
			} catch (FileNotFoundException ex) {
				throw new IllegalStateException(ex);
			}
		} else {
			// Protect the System.out from accidental closing
			try {
				out = new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
			} catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}

		return OutputFormatFactory.createFormatInstance(out, options.verbosity().orElse(Defaults.VERBOSITY));
	}

	private static String getBenchmarkName(FrameworkMethod it) {
		return it.getDeclaringClass().getName() + "." + it.getName();
	}

	/**
	 * {@link OutputFormat} that delegates to another {@link OutputFormat} and notifies {@link RunNotifier} about the
	 * progress.
	 */
	static class NotifyingOutputFormat implements OutputFormat {

		private final RunNotifier notifier;
		private final Function<String, Description> descriptionResolver;
		private final OutputFormat delegate;
		private final List<String> log = new CopyOnWriteArrayList<>();

		private volatile String lastKnownBenchmark;
		private volatile boolean recordOutput;

		NotifyingOutputFormat(RunNotifier notifier, Function<String, Description> methods, OutputFormat delegate) {
			this.notifier = notifier;
			this.descriptionResolver = methods;
			this.delegate = delegate;
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#iteration(org.openjdk.jmh.infra.BenchmarkParams, org.openjdk.jmh.infra.IterationParams, int)
		 */
		@Override
		public void iteration(BenchmarkParams benchParams, IterationParams params, int iteration) {
			delegate.iteration(benchParams, params, iteration);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#iterationResult(org.openjdk.jmh.infra.BenchmarkParams, org.openjdk.jmh.infra.IterationParams, int, org.openjdk.jmh.results.IterationResult)
		 */
		@Override
		public void iterationResult(BenchmarkParams benchParams, IterationParams params, int iteration,
				IterationResult data) {
			delegate.iterationResult(benchParams, params, iteration, data);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#startBenchmark(org.openjdk.jmh.infra.BenchmarkParams)
		 */
		@Override
		public void startBenchmark(BenchmarkParams benchParams) {

			log.clear();

			lastKnownBenchmark = benchParams.getBenchmark();
			notifier.fireTestStarted(descriptionResolver.apply(benchParams.getBenchmark()));

			delegate.startBenchmark(benchParams);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#endBenchmark(org.openjdk.jmh.results.BenchmarkResult)
		 */
		@Override
		public void endBenchmark(BenchmarkResult result) {

			recordOutput = false;
			String lastKnownBenchmark = this.lastKnownBenchmark;
			if (result != null) {
				notifier.fireTestFinished(descriptionResolver.apply(result.getParams().getBenchmark()));
			} else if (lastKnownBenchmark != null) {

				String output = StringUtils.collectionToDelimitedString(log, System.getProperty("line.separator"));
				notifier.fireTestFailure(
						new Failure(descriptionResolver.apply(lastKnownBenchmark), new JmhRunnerException(output)));
			}

			log.clear();
			delegate.endBenchmark(result);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#startRun()
		 */
		@Override
		public void startRun() {
			delegate.startRun();
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#endRun(java.util.Collection)
		 */
		@Override
		public void endRun(Collection<RunResult> result) {
			delegate.endRun(result);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#print(java.lang.String)
		 */
		@Override
		public void print(String s) {
			delegate.print(s);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#println(java.lang.String)
		 */
		@Override
		public void println(String s) {

			if (recordOutput && StringUtils.hasText(s)) {
				log.add(s);
			}

			if (s.equals("<failure>")) {
				recordOutput = true;
			}

			delegate.println(s);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#flush()
		 */
		@Override
		public void flush() {
			delegate.flush();
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#close()
		 */
		@Override
		public void close() {
			delegate.close();
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#verbosePrintln(java.lang.String)
		 */
		@Override
		public void verbosePrintln(String s) {
			delegate.verbosePrintln(s);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#write(int)
		 */
		@Override
		public void write(int b) {
			delegate.write(b);
		}

		/*
		 * (non-Javadoc)
		 * @see org.openjdk.jmh.runner.format.OutputFormat#write(byte[])
		 */
		@Override
		public void write(byte[] b) throws IOException {
			delegate.write(b);
		}
	}

	/**
	 * Exception proxy without stack trace.
	 */
	static class JmhRunnerException extends RuntimeException {

		private static final long serialVersionUID = -1385006784559013618L;

		JmhRunnerException(String message) {
			super(message);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Throwable#fillInStackTrace()
		 */
		@Override
		public synchronized Throwable fillInStackTrace() {
			return null;
		}
	}

	/**
	 * Cache {@link Function} for benchmark names to {@link Description}.
	 */
	static class CacheFunction implements Function<String, Description> {

		private final Map<String, FrameworkMethod> methodMap = new ConcurrentHashMap<>();
		private final Collection<FrameworkMethod> methods;
		private final Function<FrameworkMethod, Description> describeFunction;

		CacheFunction(Collection<FrameworkMethod> methods, Function<FrameworkMethod, Description> describeFunction) {
			this.methods = methods;
			this.describeFunction = describeFunction;
		}

		/**
		 * Resolve a benchmark name (fqcn + "." + method name) to a {@link Description}.
		 *
		 * @param benchmarkName
		 * @return
		 */
		public Description apply(String benchmarkName) {

			FrameworkMethod frameworkMethod = methodMap.computeIfAbsent(benchmarkName, key -> {

				Optional<FrameworkMethod> method = methods.stream().filter(it -> getBenchmarkName(it).equals(key)).findFirst();

				return method.orElseThrow(() -> new IllegalArgumentException(
						String.format("Cannot resolve %s to a FrameworkMethod!", benchmarkName)));
			});

			return describeFunction.apply(frameworkMethod);
		}
	}

}
