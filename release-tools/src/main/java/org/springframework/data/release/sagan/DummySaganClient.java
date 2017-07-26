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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Dummy implementation of {@link SaganClient} to just dump the request payloads into {@link System#out} instead of
 * communicating with a real server.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class DummySaganClient implements SaganClient {

	Logger logger;
	ObjectWriter mapper;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#updateProjectMetadata(org.springframework.data.release.model.Project, org.springframework.data.release.sagan.MaintainedVersions)
	 */
	@Override
	public void updateProjectMetadata(Project project, MaintainedVersions versions) {

		logger.log(project, "Updating released version on Sagan to %s!", versions);

		List<ProjectMetadata> payload = versions.stream() //
				.map(it -> new ProjectMetadata(it, versions)) //
				.collect(Collectors.toList());

		try {
			System.out.println(mapper.writeValueAsString(payload));
		} catch (JsonProcessingException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#getProjectMetadata(org.springframework.data.release.sagan.MaintainedVersion)
	 */
	@Override
	public String getProjectMetadata(MaintainedVersion version) {

		try {
			return mapper.writeValueAsString(new ProjectMetadata(version, MaintainedVersions.of(version)));
		} catch (JsonProcessingException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.release.sagan.SaganClient#getProjectMetadata(org.springframework.data.release.model.Project)
	 */
	@Override
	public String getProjectMetadata(Project project) {
		return null;
	}
}
