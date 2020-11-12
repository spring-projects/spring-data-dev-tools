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

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.lang.reflect.Field;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object to represent an individual release train iteration.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Value
@EqualsAndHashCode(of = "name")
public class Iteration implements Comparable<Iteration> {

	public static final Iteration SR24 = new Iteration("SR23", null);
	public static final Iteration SR23 = new Iteration("SR23", SR24);
	public static final Iteration SR22 = new Iteration("SR22", SR23);
	public static final Iteration SR21 = new Iteration("SR21", SR22);
	public static final Iteration SR20 = new Iteration("SR20", SR21);
	public static final Iteration SR19 = new Iteration("SR19", SR20);
	public static final Iteration SR18 = new Iteration("SR18", SR19);
	public static final Iteration SR17 = new Iteration("SR17", SR18);
	public static final Iteration SR16 = new Iteration("SR16", SR17);
	public static final Iteration SR15 = new Iteration("SR15", SR16);
	public static final Iteration SR14 = new Iteration("SR14", SR15);
	public static final Iteration SR13 = new Iteration("SR13", SR14);
	public static final Iteration SR12 = new Iteration("SR12", SR13);
	public static final Iteration SR11 = new Iteration("SR11", SR12);
	public static final Iteration SR10 = new Iteration("SR10", SR11);
	public static final Iteration SR9 = new Iteration("SR9", SR10);
	public static final Iteration SR8 = new Iteration("SR8", SR9);
	public static final Iteration SR7 = new Iteration("SR7", SR8);
	public static final Iteration SR6 = new Iteration("SR6", SR7);
	public static final Iteration SR5 = new Iteration("SR5", SR6);
	public static final Iteration SR4 = new Iteration("SR4", SR5);
	public static final Iteration SR3 = new Iteration("SR3", SR4);
	public static final Iteration SR2 = new Iteration("SR2", SR3);
	public static final Iteration SR1 = new Iteration("SR1", SR2);
	public static final Iteration GA = new Iteration("GA", SR1);
	public static final Iteration RC4 = new Iteration("RC4", GA);
	public static final Iteration RC3 = new Iteration("RC3", GA);
	public static final Iteration RC2 = new Iteration("RC2", GA);
	public static final Iteration RC1 = new Iteration("RC1", RC2);
	public static final Iteration M7 = new Iteration("M7", RC1);
	public static final Iteration M6 = new Iteration("M6", RC1);
	public static final Iteration M5 = new Iteration("M5", RC1);
	public static final Iteration M4 = new Iteration("M4", RC1);
	public static final Iteration M3 = new Iteration("M3", RC1);
	public static final Iteration M2 = new Iteration("M2", M3);
	public static final Iteration M1 = new Iteration("M1", M2);

	private static final int GREATER_THAN = 1;
	private static final int LESS_THAN = -GREATER_THAN;
	private static final int EQUAL = 0;

	/**
	 * The name of the iteration.
	 */
	private final @NonNull String name;
	private final Iteration next;

	Iteration(String name, Iteration next) {

		Assert.hasText(name, "Name must not be null or empty!");

		this.name = name;
		this.next = next;
	}

	/**
	 * Lookup {@link Iteration} by its string value.
	 *
	 * @param iteration
	 * @return
	 * @throws IllegalArgumentException if iteration cannot be resolved.
	 */
	public static Iteration valueOf(String iteration) {

		Field field = ReflectionUtils.findField(Iteration.class, iteration);

		if (field == null) {
			throw new IllegalArgumentException("Cannot resolve iteration " + iteration);
		}

		return (Iteration) ReflectionUtils.getField(field, null);
	}

	public boolean isGAIteration() {
		return this.equals(GA);
	}

	/**
	 * Returns whether the {@link Iteration} is considered public, i.e. the artifacts produced by the iteration are
	 * supposed to be published to Maven Central.
	 *
	 * @return
	 */
	public boolean isPublic() {
		return isServiceIteration() || this.equals(GA);
	}

	/**
	 * Returns whether the current Iteration is going to produce a preview release, i.e. a milestone or release candidate.
	 *
	 * @return
	 */
	public boolean isPreview() {
		return !isPublic();
	}

	public boolean isServiceIteration() {
		return name.startsWith("SR");
	}

	public boolean isNext(Iteration iteration) {
		return next.equals(iteration);
	}

	public boolean isInitialIteration() {
		return this.equals(M1);
	}

	public boolean isMilestone() {
		return name.startsWith("M");
	}

	public boolean isReleaseCandidate() {
		return name.startsWith("RC");
	}

	public int getIterationValue() {

		if (isMilestone()) {
			return Integer.parseInt(name.substring(1));
		}

		if (isReleaseCandidate() || isServiceIteration()) {
			return Integer.parseInt(name.substring(2));
		}

		return EQUAL;
	}

	public int getBugfixValue() {
		return name.startsWith("SR") ? Integer.parseInt(name.substring(2)) : 0;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(Iteration o) {

		if (isMilestone()) {

			if (o.isMilestone()) {
				return Integer.compare(getIterationValue(), o.getIterationValue());
			}

			return LESS_THAN;
		}

		if (isReleaseCandidate()) {

			if (o.isMilestone()) {
				return GREATER_THAN;
			}

			if (o.isReleaseCandidate()) {
				return Integer.compare(getIterationValue(), o.getIterationValue());
			}

			return LESS_THAN;
		}

		if (isGAIteration()) {

			if (o.isMilestone() || o.isReleaseCandidate()) {
				return GREATER_THAN;
			}

			if (o.isGAIteration()) {
				return EQUAL;
			}

			return LESS_THAN;
		}

		if (isServiceIteration()) {

			if (o.isMilestone() || o.isReleaseCandidate() || o.isGAIteration()) {
				return GREATER_THAN;
			}

			if (o.isServiceIteration()) {
				return Integer.compare(getIterationValue(), o.getIterationValue());
			}
		}

		return EQUAL;
	}
}
