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
package org.springframework.data.release.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.Value;

import org.springframework.shell.support.util.OsUtils;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@Value
public class Train implements Iterable<Module> {

	private final String name;;
	private final List<Module> modules;
	private final Iterations iterations;

	public Train(String name, Module... modules) {

		this.name = name;
		this.modules = Arrays.asList(modules);
		this.iterations = Iterations.DEFAULT;
	}

	public Train(String name, List<Module> modules) {

		this.name = name;
		this.modules = Collections.unmodifiableList(modules);
		this.iterations = Iterations.DEFAULT;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Module> iterator() {
		return modules.iterator();
	}

	public Module getModule(String name) {

		for (Module module : this) {
			if (module.getProject().getName().equals(name)) {
				return module;
			}
		}

		return null;
	}

	public Module getModule(Project project) {

		for (Module module : this) {
			if (module.getProject().equals(project)) {
				return module;
			}
		}

		return null;
	}

	public Train next(String name, Transition transition) {

		List<Module> nextModules = new ArrayList<>();

		for (Module module : modules) {
			nextModules.add(module.next(transition));
		}

		return new Train(name, nextModules);
	}

	public ModuleIteration getModuleIteration(Iteration iteration, String moduleName) {

		for (Module module : this) {
			if (module.hasName(moduleName)) {
				return new ModuleIteration(module, iteration, this);
			}
		}

		return null;
	}

	public Iterable<ModuleIteration> getModuleIterations(Iteration iteration) {
		return getModuleIterations(iteration, new Project[0]);
	}

	public Iterable<ModuleIteration> getModuleIterations(Iteration iteration, Project... exclusions) {

		List<ModuleIteration> iterations = new ArrayList<>(modules.size());
		List<Project> exclusionList = Arrays.asList(exclusions);

		for (Module module : this) {

			if (exclusionList.contains(module.getProject())) {
				continue;
			}

			iterations.add(new ModuleIteration(module, iteration, this));
		}

		return iterations;
	}

	public Iteration getIteration(String name) {
		return iterations.getIterationByName(name);
	}

	public ArtifactVersion getModuleVersion(Project project, Iteration iteration) {

		Module module = getModule(project);
		return ArtifactVersion.from(new ModuleIteration(module, iteration, this));
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append(name).append(OsUtils.LINE_SEPARATOR).append(OsUtils.LINE_SEPARATOR);
		builder.append(StringUtils.collectionToDelimitedString(modules, OsUtils.LINE_SEPARATOR));

		return builder.toString();
	}
}
