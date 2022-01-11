/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.release.issues.github;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.data.release.issues.github.LabelFactories.LabelFactory;
import org.springframework.data.util.Streamable;

/**
 * GitHub label configurations.
 *
 * @author Mark Paluch
 */
class LabelConfiguration implements Streamable<Label> {

	public static final Label TYPE_DEPENDENCY_UPGRADE = LabelFactories.TYPE_LABEL.apply("dependency-upgrade",
			"A dependency upgrade");

	public static final Label TYPE_TASK = LabelFactories.TYPE_LABEL.apply("task", "A general task");

	private final Set<Label> labels;

	private LabelConfiguration(Consumer<LabelConfigurer> configurerConsumer) {

		LabelConfigurer configurer = new LabelConfigurer();
		configurerConsumer.accept(configurer);
		this.labels = configurer.labels;
	}

	private LabelConfiguration(Set<Label> labels) {
		this.labels = labels;
	}

	/**
	 * Create an empty {@link LabelConfiguration} without any labels.
	 *
	 * @return
	 */
	public static LabelConfiguration empty() {
		return new LabelConfiguration(Collections.emptySet());
	}

	/**
	 * Create a new {@link LabelConfiguration} by merging this and the outcome of {@code consumerConfigurer}.
	 *
	 * @param configurerConsumer
	 * @return
	 */
	public LabelConfiguration mergeWith(Consumer<LabelConfigurer> configurerConsumer) {
		return mergeWith(LabelConfiguration.of(configurerConsumer));
	}

	/**
	 * Create a new {@link LabelConfiguration} by merging this and {@link LabelConfiguration other configuration}.
	 *
	 * @param other
	 * @return
	 */
	public LabelConfiguration mergeWith(LabelConfiguration other) {

		Set<Label> merged = new HashSet<>(this.labels);
		merged.addAll(other.labels);

		return new LabelConfiguration(merged);
	}

	/**
	 * Create a new {@link LabelConfiguration} applying {@code configurerConsumer}.
	 *
	 * @param configurerConsumer
	 * @return
	 */
	public static LabelConfiguration of(Consumer<LabelConfigurer> configurerConsumer) {
		return new LabelConfiguration(configurerConsumer);
	}

	/**
	 * Common label configuration.
	 *
	 * @return
	 */
	public static LabelConfiguration commonLabels() {

		LabelConfiguration configuration = LabelConfiguration.of(configurer -> {

			configurer.register(LabelFactories.FOR_LABEL, "external-project",
					"For an external project and not something we can fix");
			configurer.register(LabelFactories.FOR_LABEL, "stackoverflow",
					"A question that's better suited to stackoverflow.com");
			configurer.register(LabelFactories.FOR_LABEL, "team-attention",
					"An issue we need to discuss as a team to make progress");
			configurer.register(LabelFactories.FOR_LABEL, "external-project", "An issue to discuss face-to-face");

			configurer.register(LabelFactories.STATUS_LABEL, "blocked",
					"An issue that's blocked on an external project change");
			configurer.register(LabelFactories.STATUS_LABEL, "declined",
					"A suggestion or change that we don't feel we should currently apply");
			configurer.register(LabelFactories.STATUS_LABEL, "duplicate", "A duplicate of another issue");
			configurer.register(LabelFactories.STATUS_LABEL, "feedback-provided", "Feedback has been provided");
			configurer.register(LabelFactories.STATUS_LABEL, "feedback-reminder",
					"We've sent a reminder that we need additional information before we can continue");
			configurer.register(LabelFactories.STATUS_LABEL, "first-timers-only",
					"An issue that can only be worked on by brand new contributors");
			configurer.register(LabelFactories.STATUS_LABEL, "ideal-for-contribution",
					"An issue that a contributor can help us with");
			configurer.register(LabelFactories.STATUS_LABEL, "invalid", "An issue that we don't feel is valid");
			configurer.register(LabelFactories.STATUS_LABEL, "on-hold", "We cannot start working on this issue yet");
			configurer.register(LabelFactories.STATUS_LABEL, "pending-design-work",
					"Needs design work before any code can be developed");
			configurer.register(LabelFactories.STATUS_LABEL, "superseded", "An issue that has been superseded by another");
			configurer.register(LabelFactories.STATUS_LABEL, "waiting-for-feedback",
					"We need additional information before we can continue");
			configurer.register(LabelFactories.STATUS_LABEL, "waiting-for-triage", "An issue we've not yet triaged");

			configurer.register(LabelFactories.TYPE_LABEL, "blocker", "An issue that is blocking us from releasing");
			configurer.register(LabelFactories.TYPE_LABEL, "bug", "A general bug");
			configurer.register(TYPE_DEPENDENCY_UPGRADE);
			configurer.register(LabelFactories.TYPE_LABEL, "documentation", "A documentation update");
			configurer.register(LabelFactories.TYPE_LABEL, "enhancement", "A general enhancement");
			configurer.register(LabelFactories.TYPE_LABEL, "regression", "A regression from a previous release");
			configurer.register(TYPE_TASK);
		});

		return configuration;
	}

	/**
	 * Retrieve a required {@link Label}.
	 *
	 * @param labelFactory
	 * @param name
	 * @return
	 * @throws IllegalArgumentException if the label was not found.
	 */
	public Label getRequiredLabel(LabelFactory labelFactory, String name) {

		Label toFind = labelFactory.apply(name, "");

		return this.labels.stream().filter(it -> it.equals(toFind)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find label %s", toFind.getName())));
	}

	@Override
	public Iterator<Label> iterator() {
		return labels.iterator();
	}

	public List<Label> getNewLabels(Collection<Label> existingLabels) {
		return labels.stream().filter(o -> !existingLabels.contains(o)).collect(Collectors.toList());
	}

	public List<Label> getExistingLabels(List<Label> existingLabels) {
		return labels.stream().filter(existingLabels::contains).collect(Collectors.toList());
	}

	public List<Label> getAdditionalLabels(List<Label> existingLabels) {

		List<Label> result = new ArrayList<>(existingLabels);

		result.removeAll(getNewLabels(existingLabels));
		result.removeAll(getExistingLabels(existingLabels));

		return result;
	}

	/**
	 * Configuration utility to register labels for a {@link LabelFactory}.
	 */
	class LabelConfigurer {

		private final Set<Label> labels = new HashSet<>();

		public void register(LabelFactory factory, String name, String description) {

			Label label = factory.apply(name, description);

			labels.add(label);
		}

		public void register(Label label) {
			labels.add(label);
		}
	}

}
