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
package org.springframework.data.release.git;

import lombok.Getter;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Train;
import org.springframework.util.Assert;

class BackportTargets implements Iterable<Branch> {

	private final @Getter Branch source;
	private final Set<Branch> targets;

	/**
	 * Creates a new {@link BackportTargets} instance for the given {@link ModuleIteration} and
	 * 
	 * @param module must not be {@literal null}.
	 * @param targets must not be {@literal null}.
	 */
	public BackportTargets(ModuleIteration module, Collection<Train> targets) {

		Assert.notNull(module, "Module iteration must not be null!");
		Assert.notNull(targets, "Target trains  must not be null!");

		this.source = Branch.from(module);

		Stream<Branch> branches = targets.stream().map(target -> target.getModuleIfAvailable(module.getProject()))//
				.flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))//
				.map(Branch::from);

		this.targets = Stream.concat(branches, source.isMasterBranch() ? Stream.empty() : Stream.of(Branch.MASTER))
				.collect(Collectors.toSet());
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Branch> iterator() {
		return targets.iterator();
	}
}
