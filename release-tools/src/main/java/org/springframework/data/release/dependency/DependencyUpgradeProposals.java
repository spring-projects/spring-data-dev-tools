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
package org.springframework.data.release.dependency;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fusesource.jansi.Ansi;

import org.springframework.data.release.model.TrainIteration;
import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;

/**
 * Value object capturing upgrade proposals for each {@link Dependency}.
 *
 * @author Mark Paluch
 */
public class DependencyUpgradeProposals {

	private final Map<Dependency, DependencyUpgradeProposal> proposals;

	public DependencyUpgradeProposals(Map<Dependency, DependencyUpgradeProposal> proposals) {
		this.proposals = proposals;
	}

	/**
	 * Create an empty {@link DependencyUpgradeProposal} object.
	 *
	 * @return
	 */
	public static DependencyUpgradeProposals empty() {
		return new DependencyUpgradeProposals(Collections.emptyMap());
	}

	/**
	 * Create a new {@link DependencyUpgradeProposal} by merging this and {@code other}.
	 *
	 * @param other
	 * @return
	 */
	public DependencyUpgradeProposals mergeWith(DependencyUpgradeProposals other) {

		Map<Dependency, DependencyUpgradeProposal> proposals = new TreeMap<>(this.proposals);
		proposals.putAll(other.proposals);

		return new DependencyUpgradeProposals(proposals);
	}

	/**
	 * Create a tabular summary including {@link Ansi} escapes.
	 *
	 * @param includeAll
	 * @return
	 */
	public Table toTable(boolean includeAll) {

		Table table = new Table();
		table.addHeader(1, new TableHeader("Dependency"));
		table.addHeader(2, new TableHeader("Current"));
		table.addHeader(3, new TableHeader("Available"));
		table.addHeader(4, new TableHeader("Proposed"));

		proposals.forEach((dependency, proposal) -> {

			boolean updateAvailable = proposal.isUpgradeAvailable();

			String s = updateAvailable
					? Ansi.ansi().fg(Ansi.Color.MAGENTA).a(proposal.getProposal()).fg(Ansi.Color.GREEN).toString()
					: proposal.getProposal().toString();

			if (includeAll || updateAvailable) {
				table.addRow(dependency.getName(), proposal.getCurrent().toString(), proposal.getNewVersions(includeAll, false),
						s);
			}
		});

		return table;
	}

	/**
	 * Return the upgrade proposal as {@link java.util.Properties} representation.
	 *
	 * @param iteration
	 * @return
	 */
	public String asProperties(TrainIteration iteration) {

		StringBuilder builder = new StringBuilder();

		builder.append("dependency.train=").append(iteration.getTrain().getName()).append(System.lineSeparator());
		builder.append("dependency.iteration=").append(iteration.getIteration().getName()).append(System.lineSeparator());

		proposals.forEach((dependency, proposal) -> {

			boolean updateAvailable = proposal.isUpgradeAvailable();

			if (updateAvailable) {
				builder.append(System.lineSeparator());
				builder.append(String.format("# %s - Available versions: ", dependency.getName()))
						.append(proposal.getNewVersions(true, true)).append(System.lineSeparator());

				builder.append(
						String.format("dependency[%s\\:%s]=%s", dependency.getGroupId(), dependency.getArtifactId(), proposal));
				builder.append(System.lineSeparator());
			}
		});

		return builder.toString();
	}

	/**
	 * Create a dependency upgrade map by parsing {@link Properties}.
	 *
	 * @param iteration
	 * @param properties
	 * @return
	 */
	public static DependencyVersions fromProperties(TrainIteration iteration, Properties properties) {

		Pattern keyPattern = Pattern.compile("dependency\\[([a-zA-Z0-9\\-\\.]+):([a-zA-Z0-9\\-\\.]+)\\]");

		String verificationTrain = properties.getProperty("dependency.train", "");
		String verificationIteration = properties.getProperty("dependency.iteration", "");

		if (!verificationTrain.equals(iteration.getTrain().getName())
				|| !verificationIteration.equals(iteration.getIteration().getName())) {
			throw new IllegalArgumentException(
					String.format("Verification failed: Dependency upgrade descriptor reports %s %s", verificationTrain,
							verificationIteration));
		}

		Map<Dependency, DependencyVersion> result = new LinkedHashMap<>();

		properties.forEach((k, v) -> {

			if ("dependency.train".equals(k) || "dependency.iteration".equals(k)) {
				return;
			}

			Matcher matcher = keyPattern.matcher(k.toString());

			if (!matcher.matches()) {
				throw new IllegalArgumentException(String.format("Unexpected key: %s", k));
			}

			String artifactId = matcher.group(2);
			Dependency dependency = Dependencies.getRequiredByArtifactId(artifactId);

			result.put(dependency, DependencyVersion.of(v.toString()));

		});

		return new DependencyVersions(result);
	}

}
