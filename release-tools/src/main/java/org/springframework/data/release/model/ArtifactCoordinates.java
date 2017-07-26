/*
 * Copyright 2015 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class ArtifactCoordinates {

	public static ArtifactCoordinates SPRING_DATA = forGroupId("org.springframework.data");

	private final String groupId;
	private final @Getter(AccessLevel.PACKAGE) List<ArtifactCoordinate> coordinates;

	public static ArtifactCoordinates forGroupId(String groupId) {
		return new ArtifactCoordinates(groupId, new ArrayList<>());
	}

	public ArtifactCoordinates artifacts(String... artifactIds) {

		return new ArtifactCoordinates(groupId, Arrays.stream(artifactIds)//
				.map(artifactId -> ArtifactCoordinate.of(groupId, artifactId))//
				.collect(Collectors.toList()));
	}

	public ArtifactCoordinates and(ArtifactCoordinate coordinate) {

		List<ArtifactCoordinate> artifacts = new ArrayList<>(coordinates);
		artifacts.add(coordinate);

		return new ArtifactCoordinates(groupId, artifacts);
	}
}
