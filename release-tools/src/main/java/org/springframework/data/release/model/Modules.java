/*
 * Copyright 2016 the original author or authors.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import org.springframework.data.release.Streamable;

/**
 * A {@link Streamable} set of modules. Makes sure the stream order will match the natural dependency order of the given
 * {@link Module} instances.
 * 
 * @author Oliver Gierke
 */
class Modules implements Streamable<Module> {

	private final Collection<Module> modules;

	private Modules(Collection<Module> modules) {
		this.modules = new TreeSet<>(modules);
	}

	public static Modules of(Collection<Module> modules) {
		return new Modules(modules);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Module> iterator() {
		return modules.iterator();
	}
}
