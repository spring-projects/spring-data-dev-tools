/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.release.deployment;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.web.util.UriTemplate;

/**
 * Information about a deployment.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class DeploymentInformation {

	private static UriTemplate REPOSITORY_TEMPLATE = new UriTemplate(
			"artifactory::default::{server};build.number={buildNumber};build.name={buildName}");

	private final @Getter @NonNull ModuleIteration module;
	private final @NonNull DeploymentProperties properties;

	/**
	 * Returns a unique build number for this particular deployment.
	 */
	private final @Getter String buildNumber = String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));

	/**
	 * Returns the name of the build.
	 * 
	 * @return
	 */
	public String getBuildName() {
		return module.getProject().getFullName().concat(" - Release");
	}

	/**
	 * Returns the name of the repository to deploy to.
	 * 
	 * @return
	 */
	public String getTargetRepository() {
		return properties.getRepositoryPrefix()
				.concat(module.getIteration().isPublic() ? "libs-release-local" : "libs-milestone-local");
	}

	/**
	 * Returns the full URL to be used as deployment target.
	 * 
	 * @return
	 */
	public String getDeploymentTargetUrl() {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("server", properties.getStagingRepositoryUrl());
		parameters.putAll(getBuildInfoParameters());

		return REPOSITORY_TEMPLATE.expand(parameters).toString();
	}

	/**
	 * Returns a {@link Map} to expand a URI template to access the build information.
	 * 
	 * @return
	 */
	public Map<String, Object> getBuildInfoParameters() {

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("buildNumber", buildNumber);
		parameters.put("buildName", getBuildName());

		return parameters;
	}
}
