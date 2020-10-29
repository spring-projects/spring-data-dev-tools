/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.release.model;

import static org.springframework.data.release.model.Iteration.*;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.With;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Train implements Streamable<Module> {

	private final String name;;
	private final Modules modules;
	private @Nullable Version calver;
	private @With Iterations iterations;
	private @With boolean alwaysUseBranch;

	public Train(String name, Module... modules) {
		this(name, Arrays.asList(modules));
	}

	public Train(String name, Collection<Module> modules) {
		this(name, Modules.of(modules), null, Iterations.DEFAULT, false);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Module> iterator() {
		return modules.iterator();
	}

	public boolean contains(Project project) {

		return modules.stream().//
				anyMatch(module -> module.getProject().equals(project));
	}

	/**
	 * Returns the {@link Module} for the given {@link Project} in the current release {@link Train}.
	 *
	 * @param project must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException in case no {@link Module} can be found for the given {@link Project} in the
	 *           current release {@link Train}.
	 */
	public Module getModule(Project project) {

		Assert.notNull(project, "Project must not be null!");

		return getModuleIfAvailable(project).orElseThrow(() -> new IllegalArgumentException(
				String.format("No module found for project %s in release train %s!", project.getName(), this.name)));
	}

	/**
	 * Returns the {@link Module} for the given {@link Project} in the current release {@link Train}.
	 *
	 * @param project must not be {@literal null}.
	 * @return the {@link Module} wrapped into an {@link Optional} if present, {@link Optional#empty()} otherwise.
	 */
	public Optional<Module> getModuleIfAvailable(Project project) {

		Assert.notNull(project, "Project must not be null!");

		return modules.stream().filter(module -> module.getProject().equals(project)).findFirst();
	}

	public Train next(String name, Transition transition, Module... additionalModules) {

		Set<Module> modules = Stream.concat(this.modules.stream(), Stream.of(additionalModules)).//
				map(module -> Arrays.stream(additionalModules).//
						reduce(module.next(transition),
								(it, additionalModule) -> it.hasSameProjectAs(additionalModule) ? additionalModule : it))
				.collect(Collectors.toSet());

		return new Train(name, Modules.of(modules), calver, iterations, alwaysUseBranch);
	}

	public Train filterModules(Predicate<Module> filterPredicate) {
		return new Train(name, Modules.of(getModules().stream().filter(filterPredicate).collect(Collectors.toList())),
				calver, iterations, alwaysUseBranch);
	}

	/**
	 * Returns the {@link ModuleIteration} for the given {@link Project} and {@link Iteration}.
	 *
	 * @param project must not be {@literal null}.
	 * @param iteration must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public ModuleIteration getModuleIteration(Project project, Iteration iteration) {

		Assert.notNull(project, "Project must not be null!");
		Assert.notNull(iteration, "Iteration must not be null!");

		Module module = getModule(project);

		return new ModuleIteration(module, getIteration(iteration));
	}

	public Iterable<ModuleIteration> getModuleIterations(Iteration iteration) {
		return getModuleIterations(iteration, new Project[0]);
	}

	List<ModuleIteration> getModuleIterations(Iteration iteration, Project... exclusions) {

		List<Project> exclusionList = Arrays.asList(exclusions);

		return modules.stream() //
				.filter(module -> !exclusionList.contains(module.getProject())) //
				.map(module -> new ModuleIteration(module, new TrainIteration(this, iteration))) //
				.collect(Collectors.toList());
	}

	public TrainIteration getIteration(String name) {
		return new TrainIteration(this, iterations.getIterationByName(name));
	}


	public ArtifactVersion getModuleVersion(Project project, Iteration iteration) {

		Module module = getModule(project);

		return ArtifactVersion.of(new ModuleIteration(module, new TrainIteration(this, iteration)));
	}

	public boolean usesCalver() {
		return calver != null;
	}

	public Train withCalver(String calverVersion) {

		Version calver = Version.parse(calverVersion);
		Set<Module> modules = this.modules.stream().map(it -> {

			if (it.getProject() == Projects.BOM) {
				return new Module(it.getProject(), calver.toMajorMinorBugfix());
			}
			return it;

		}).collect(Collectors.toSet());

		return new Train(name, Modules.of(modules), calver, iterations, alwaysUseBranch);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append(name).//
				append(IOUtils.LINE_SEPARATOR).//
				append(IOUtils.LINE_SEPARATOR);

		builder.append(modules.stream().//
				map(Module::toString).//
				collect(Collectors.joining(IOUtils.LINE_SEPARATOR)));

		return builder.toString();
	}

	/**
	 * Returns the {@link TrainIteration} for the given {@link Iteration} contained in this train.
	 *
	 * @param iteration must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException in case the given {@link Iteration} is not available in this {@link Train}.
	 */
	public TrainIteration getIteration(Iteration iteration) {

		Assert.isTrue(iterations.contains(iteration),
				String.format("Iteration %s is not a valid one for the configured iterations %s!", iteration, iterations));

		return new TrainIteration(this, iteration);
	}

	/**
	 * Value object to represent a set of {@link Iteration}s.
	 *
	 * @author Oliver Gierke
	 */
	@EqualsAndHashCode
	@ToString
	public static class Iterations implements Iterable<Iteration> {

		public static Iterations DEFAULT = new Iterations(M1, M2, M3, RC1, RC2, GA, SR1, SR2, SR3, SR4, SR5, SR6, SR7, SR8,
				SR9, SR10,
				SR11, SR12, SR13, SR14, SR15, SR16, SR17, SR18, SR19, SR20, SR21, SR22, SR23, SR24);

		private final List<Iteration> iterations;

		/**
		 * Creates a new {@link Iterations} from the given {@link Iteration}.
		 *
		 * @param iterations
		 */
		Iterations(Iteration... iterations) {
			this.iterations = Arrays.asList(iterations);
		}

		/**
		 * Returns the iteration with the given name.
		 *
		 * @param name must not be {@literal null} or empty.
		 * @return
		 */
		Iteration getIterationByName(String name) {

			Assert.hasText(name, "Name must not be null or empty!");

			return iterations.stream().//
					filter(iteration -> iteration.getName().equalsIgnoreCase(name)).//
					findFirst()
					.orElseThrow(() -> new IllegalArgumentException(String.format("No iteration found with name %s!", name)));
		}

		Iteration getPreviousIteration(Iteration iteration) {

			return iterations.stream().//
					filter(candidate -> candidate.isNext(iteration)).//
					findFirst().orElseThrow(() -> new IllegalArgumentException(
							String.format("Could not find previous iteration for %s!", iteration)));
		}

		boolean contains(Iteration iteration) {
			return iterations.contains(iteration);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<Iteration> iterator() {
			return iterations.iterator();
		}
	}
}
