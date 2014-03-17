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

import static org.springframework.data.release.model.Iteration.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lombok.Value;

import org.springframework.util.Assert;

/**
 * Value object to represent a set of {@link Iteration}s.
 * 
 * @author Oliver Gierke
 */
@Value
public class Iterations implements Iterable<Iteration> {

	public static Iterations DEFAULT = new Iterations(M1, RC1, GA, SR1, SR2, SR3, SR4);

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
	public Iteration getIterationByName(String name) {

		Assert.hasText(name, "Name must not be null or empty!");

		for (Iteration iteration : this) {
			if (iteration.getName().equalsIgnoreCase(name)) {
				return iteration;
			}
		}

		return null;
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
