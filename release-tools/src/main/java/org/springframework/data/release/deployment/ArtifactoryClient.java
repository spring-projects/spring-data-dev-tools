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

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.IOException;
import java.net.URI;

import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A client to interact with Artifactory.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
class ArtifactoryClient {

	private final RestOperations template;
	private final Logger logger;
	private final DeploymentProperties properties;

	/**
	 * Triggers the promotion of the artifacts identified by the given {@link DeploymentInformation}.
	 * 
	 * @param information must not be {@literal null}.
	 */
	public void promote(DeploymentInformation information) {

		Assert.notNull(information, "DeploymentInformation must not be null!");

		ModuleIteration module = information.getModule();
		URI uri = properties.getServer().getPromotionResource(information);

		logger.log(module, "Promoting %s %s from %s to %s.", information.getBuildName(), information.getBuildNumber(),
				properties.getStagingRepository(), information.getTargetRepository());

		try {
			template.postForEntity(uri, new PromotionRequest(information.getTargetRepository()), String.class);
		} catch (HttpClientErrorException o_O) {
			handle("Promotion failed!", o_O, module);
		}
	}

	public void verify() {

		URI verificationResource = properties.getServer().getVerificationResource();

		try {

			logger.log("Artifactory", "Verifying authentication using a GET call to %s.", verificationResource);

			template.getForEntity(verificationResource, String.class);

			logger.log("Artifactory", "Authentication verified!");

		} catch (HttpClientErrorException o_O) {
			handle("Authentication verification failed!", o_O);
		}
	}

	private void handle(String log, HttpClientErrorException o_O, ModuleIteration module) {

		try {

			logger.log(module, log);

			Errors errors = new ObjectMapper().readValue(o_O.getResponseBodyAsByteArray(), Errors.class);
			errors.getErrors().forEach(error -> logger.log(module, error));
			errors.getMessages().forEach(message -> logger.log(module, message));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handle(String log, HttpClientErrorException o_O) {

		try {

			logger.log("Artifactory Client", log);

			Errors errors = new ObjectMapper().readValue(o_O.getResponseBodyAsByteArray(), Errors.class);
			errors.getErrors().forEach(error -> logger.log("Artifactory Client", error));
			errors.getMessages().forEach(message -> logger.log("Artifactory Client", message));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteArtifacts(DeploymentInformation information) {
		template.delete(properties.getServer().getDeleteBuildResource(information));
	}

	@Value
	private static class PromotionRequest {
		String targetRepo;
	}
}
