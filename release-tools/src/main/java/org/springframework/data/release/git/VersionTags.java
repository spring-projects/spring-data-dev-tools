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
package org.springframework.data.release.git;

import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.util.Pair;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Value object to represent a collection of {@link Tag}s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@EqualsAndHashCode
public class VersionTags implements Streamable<Tag> {

	private final Project project;
	private final List<Tag> tags;

	/**
	 * Creates a new {@link VersionTags} instance for the given {@link Project} and {@link List} of {@link Tag}s.
	 *
	 * @param project must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 */
	VersionTags(Project project, Collection<Tag> source) {

		Assert.notNull(project, "Project must not be null!");
		Assert.notNull(source, "Tags must not be null!");

		this.project = project;
		this.tags = source.stream().//
				filter(Tag::isVersionTag).//
				sorted().collect(Collectors.toList());
	}

	/**
	 * Returns the latest {@link Tag}.
	 *
	 * @return
	 */
	public Tag getLatest() {
		return tags.get(tags.size() - 1);
	}

	public Tag createTag(ModuleIteration iteration) {

		if (iteration.getProject().equals(Projects.BOM)) {
			return Tag.of(iteration.getTrainIteration().getReleaseTrainNameAndVersion());
		}

		Tag latest = getLatest();
		ArtifactVersion version = ArtifactVersion.of(iteration);

		if (latest != null) {
			return latest.createNew(version);
		}

		return Tag.of(version.toString());
	}

	/**
	 * Returns all {@link Tag}s as {@link List}.
	 *
	 * @return
	 */
	public List<Tag> asList() {
		return tags;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Tag> iterator() {
		return tags.iterator();
	}

	/**
	 * Create {@link VersionTags} that contains only tags from the given {@link Train}.
	 *
	 * @param train
	 * @return
	 */
	public VersionTags filter(Train train) {
		return filter((tag, ti) -> ti.getTrain().equals(train));
	}

	/**
	 * Create {@link VersionTags} from a filtered subset of this {@link VersionTags}.
	 *
	 * @param predicate
	 * @return
	 */
	public VersionTags filter(BiPredicate<Tag, TrainIteration> predicate) {

		Map<Tag, TrainIteration> iterations = newMap();

		withIterations().forEach((tag, trainIteration) -> {

			if (predicate.test(tag, trainIteration)) {
				iterations.put(tag, trainIteration);
			}
		});

		return new VersionTags(project, iterations.keySet());
	}

	Map<Tag, TrainIteration> withIterations() {

		Map<Tag, TrainIteration> iterations = newMap();

		for (Tag tag : tags) {

			Optional<ArtifactVersion> artifactVersion = tag.toArtifactVersion();
			if (!artifactVersion.isPresent()) {
				continue;
			}

			ArtifactVersion version = artifactVersion.get();
			Optional<Iteration> iteration = toIteration(version);

			if (!iteration.isPresent()) {
				continue;
			}

			Optional<TrainIteration> ti = getTrainIteration(version, project, iteration.get());

			ti.ifPresent(it -> iterations.put(tag, it));
		}

		return iterations;
	}

	public Optional<TrainIteration> findMostRecentTrainIterationBefore(Iteration iterationToFind) {
		return find((tag, iteration) -> iteration.getIteration().compareTo(iterationToFind) < 0, Pair::getSecond);
	}

	public Optional<Tag> findTag(Iteration iterationToFind) {
		return find((tag, iteration) -> iteration.getIteration().compareTo(iterationToFind) == 0, Pair::getFirst);
	}

	public <T> Optional<T> find(BiPredicate<Tag, TrainIteration> filter,
			Function<Pair<Tag, TrainIteration>, T> resultExtractor) {

		for (Map.Entry<Tag, TrainIteration> entry : withIterations().entrySet()) {

			if (filter.test(entry.getKey(), entry.getValue())) {
				return Optional.of(resultExtractor.apply(Pair.of(entry.getKey(), entry.getValue())));
			}
		}

		return Optional.empty();
	}

	private static <K, V> TreeMap<K, V> newMap() {
		return new TreeMap(Comparator.reverseOrder());
	}

	private static Optional<TrainIteration> getTrainIteration(ArtifactVersion version, Project project,
			Iteration iteration) {

		// consider major version bump during ReleaseTrain
		boolean minorIncrementedButTargetsNextTrain = false;

		for (Train train : ReleaseTrains.trains()) {

			TrainIteration ti = new TrainIteration(train, iteration);

			if (!ti.contains(project)) {
				continue;
			}

			if (minorIncrementedButTargetsNextTrain) {
				return Optional.of(ti);
			}

			ModuleIteration mi = ti.getModule(project);

			ArtifactVersion artifactVersion = ArtifactVersion.of(mi);

			if (artifactVersion.equals(version)) {
				return Optional.of(ti);
			}

			if (artifactVersion.getNextMinorVersion().equals(version)) {
				minorIncrementedButTargetsNextTrain = true;
			}
		}

		return Optional.empty();
	}

	private static Optional<Iteration> toIteration(ArtifactVersion version) {

		if (version.isMilestoneVersion()) {
			return Optional.of(Iteration.valueOf("M" + version.getLevel()));
		}

		if (version.isReleaseCandidateVersion()) {
			return Optional.of(Iteration.valueOf("RC" + version.getLevel()));
		}

		if (version.isBugFixVersion()) {
			return Optional.of(Iteration.valueOf("SR" + version.getLevel()));
		}

		if (version.isReleaseVersion()) {
			return Optional.of(Iteration.GA);
		}

		return Optional.empty();
	}
}
