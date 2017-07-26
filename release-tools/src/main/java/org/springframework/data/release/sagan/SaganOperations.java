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
package org.springframework.data.release.sagan;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.git.Tag;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.Version;
import org.springframework.data.release.utils.ListWrapperCollector;
import org.springframework.data.release.utils.Logger;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class SaganOperations {

	private static final List<Project> TO_FILTER = Arrays.asList(Projects.COMMONS, //
			Projects.GEODE, //
			Projects.KEY_VALUE);

	GitOperations git;
	SaganClient client;
	Logger logger;

	/**
	 * Updates the project metadata for the modules in the given release {@link Train}s.
	 * 
	 * @param trains must not be {@literal null}.
	 */
	void updateProjectMetadata(Train... trains) {

		Assert.notNull(trains, "Trains must not be null!");

		updateProjectMetadata(Arrays.asList(trains));
	}

	void updateProjectMetadata(List<Train> trains) {

		Assert.notNull(trains, "Trains must not be null!");

		findVersions(trains).forEach(client::updateProjectMetadata);
	}

	/**
	 * Returns all {@link MaintainedVersions} grouped by {@link Project} for the given release {@link Train}.
	 * 
	 * @param trains must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	Map<Project, MaintainedVersions> findVersions(Train... trains) {

		Assert.notNull(trains, "Trains must not be null!");

		return findVersions(Arrays.asList(trains));
	}

	Map<Project, MaintainedVersions> findVersions(List<Train> trains) {

		Assert.notNull(trains, "Trains must not be null!");

		return trains.stream() //
				.flatMap(train -> train.stream()//
						.filter(module -> !TO_FILTER.contains(module.getProject())) //
						.map(module -> getLatestVersion(module, train)) //
						.flatMap(MaintainedVersion::all)) //
				.collect(
						Collectors.groupingBy(it -> it.getProject(), ListWrapperCollector.collectInto(MaintainedVersions::of)));
	}

	private MaintainedVersion getLatestVersion(Module module, Train train) {

		Project project = module.getProject();

		MaintainedVersion version = git.getTags(project).stream()//
				.filter(tag -> matches(tag, module.getVersion())) //
				.sorted(Comparator.reverseOrder()) //
				.findFirst() //
				.flatMap(tag -> tag.toArtifactVersion()) //
				.map(it -> MaintainedVersion.of(module.getProject(), it, train)) //
				.orElseGet(() -> MaintainedVersion.snapshot(module, train));

		logger.log(project, "Found version %s for train %s!", version.getVersion(), train.getName());

		return version;
	}

	/**
	 * Returns whether the given {@link Tag} is one that logically belongs to the given version.
	 * 
	 * @param tag must not be {@literal null}.
	 * @param version must not be {@literal null}.
	 * @return
	 */
	private static boolean matches(Tag tag, Version version) {

		return tag.toArtifactVersion()//
				.map(it -> it.isVersionWithin(version))//
				.orElse(false);
	}
}
