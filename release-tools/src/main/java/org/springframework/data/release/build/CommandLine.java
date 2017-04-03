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
package org.springframework.data.release.build;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.release.model.Masked;
import org.springframework.util.Assert;

/**
 * Value object to represent a Maven command line.
 * 
 * @author Oliver Gierke
 */
@Value
class CommandLine {

	@NonNull List<Goal> goals;
	@NonNull List<Argument> arguments;

	/**
	 * Creates a new {@link CommandLine} for the given {@link Goal} and {@link Argument}s.
	 * 
	 * @param goal must not be {@literal null}.
	 * @param argument must not be {@literal null}.
	 * @return
	 */
	public static CommandLine of(Goal goal, Argument... argument) {
		return new CommandLine(Collections.singletonList(goal), Arrays.asList(argument));
	}

	/**
	 * Creates a new {@link CommandLine} for the given {@link Goal}s and {@link Argument}s.
	 * 
	 * @param goal must not be {@literal null}.
	 * @param argument must not be {@literal null}.
	 * @return
	 */
	public static CommandLine of(Goal first, Goal second, Argument... argument) {
		return new CommandLine(Arrays.asList(first, second), Arrays.asList(argument));
	}

	/**
	 * Returns a new {@link CommandLine} with the given {@link Argument} added in case the given {@link BooleanSupplier}
	 * evaluates to {@literal true}.
	 * 
	 * @param argument must not be {@literal null}.
	 * @param condition must not be {@literal null}.
	 * @return
	 */
	public CommandLine conditionalAnd(Argument argument, BooleanSupplier condition) {
		return condition.getAsBoolean() ? and(argument) : this;
	}

	/**
	 * Returns a new {@link CommandLine} with the given {@link Argument} added.
	 * 
	 * @param argument must not be {@literal null}.
	 * @return
	 */
	public CommandLine and(Argument argument) {

		Assert.notNull(argument, "Argument must not be null!");

		List<Argument> newArguments = new ArrayList<Argument>(arguments.size() + 1);
		newArguments.addAll(arguments);
		newArguments.add(argument);

		return new CommandLine(goals, newArguments);
	}

	/**
	 * Renders the current {@link CommandLine} as a plain {@link List} of {@link String}s using the given {@link Function}
	 * to expand the {@link Goal}s.
	 * 
	 * @param goalExpansion must not be {@literal null}.
	 * @return
	 */
	public List<String> toCommandLine(Function<Goal, String> goalExpansion) {

		Stream<String> goalStream = goals.stream().map(goalExpansion);
		Stream<String> argumentStream = arguments.stream().map(it -> it.toCommandLineArgument());

		return Stream.concat(goalStream, argumentStream).collect(Collectors.toList());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		Stream<String> goalStream = goals.stream().map(it -> it.getGoal());
		Stream<String> argumentStream = arguments.stream().map(Object::toString);

		return Stream.concat(goalStream, argumentStream).collect(Collectors.joining(" "));
	}

	/**
	 * Represents a Maven goal to invoke. Can be a custom one but also one of the predefined instances.
	 *
	 * @author Oliver Gierke
	 */
	@Value(staticConstructor = "goal")
	public static class Goal {

		public static final Goal CLEAN = Goal.goal("clean");
		public static final Goal INSTALL = Goal.goal("install");
		public static final Goal DEPLOY = Goal.goal("deploy");
		public static final Goal VALIDATE = Goal.goal("validate");

		String goal;
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Argument {

		public static Argument SKIP_TESTS = Argument.arg("skipTests");

		@NonNull String name;
		@NonNull Optional<ArgumentValue<?>> value;

		private Argument(String name, ArgumentValue<?> value) {
			this(name, Optional.of(value));
		}

		static Argument of(String name) {
			return new Argument(name, Optional.empty());
		}

		/**
		 * Enables the given comma-separated profiles for the {@link CommandLine}.
		 * 
		 * @param name must not be {@literal null} or empty.
		 * @return
		 */
		public static Argument profile(String name, String... others) {

			Assert.hasText(name, "Profiles must not be null or empty!");
			Assert.notNull(others, "Other profiles must not be null!");

			String profiles = Stream.concat(Stream.of(name), Arrays.stream(others)).collect(Collectors.joining(","));

			return Argument.of("-P".concat(profiles));
		}

		public static Argument arg(String name) {
			return Argument.of("-D".concat(name));
		}

		public Argument withValue(Object value) {
			return new Argument(name, ArgumentValue.of(value));
		}

		public Argument withQuotedValue(Object value) {
			return new Argument(name, ArgumentValue.of(value, it -> String.format("\"%s\"", it.toString())));
		}

		public Argument withValue(Masked masked) {
			return new Argument(name, ArgumentValue.of(masked));
		}

		public String toCommandLineArgument() {
			return toNameValuePair(value.map(ArgumentValue::toCommandLine));
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return toNameValuePair(value.map(Object::toString));
		}

		private String toNameValuePair(Optional<String> source) {

			return source//
					.map(it -> String.format("%s=%s", name, it))//
					.orElse(name);
		}

		@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
		private static class ArgumentValue<T> {

			private final @NonNull T value;
			private final @NonNull Optional<Function<T, String>> preparer;
			private final @NonNull Optional<Function<T, String>> toString;

			public static <T> ArgumentValue<T> of(T value) {
				return new ArgumentValue<>(value, Optional.empty(), Optional.empty());
			}

			public static <T> ArgumentValue<T> of(T value, Function<T, String> preparer) {
				return new ArgumentValue<>(value, Optional.of(preparer), Optional.empty());
			}

			/**
			 * Returns an {@link ArgumentValue} for the given {@link Masked} value.
			 * 
			 * @param masked must not be {@literal null}.
			 * @return
			 */
			public static <T extends Masked> ArgumentValue<T> of(T masked) {
				return new ArgumentValue<>(masked, Optional.empty(), Optional.of(it -> it.masked()));
			}

			/**
			 * Returns the {@link String} variant of the argument value.
			 * 
			 * @return
			 */
			public String toCommandLine() {
				return preparer.map(it -> it.apply(value)).orElseGet(() -> value.toString());
			}

			/*
			 * (non-Javadoc)
			 * @see java.lang.Object#toString()
			 */
			public String toString() {
				return toString.map(it -> it.apply(value)).orElseGet(() -> toCommandLine());
			}
		}
	}
}
