/*
 * Copyright 2015-2022 the original author or authors.
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
package org.springframework.data.release.deployment;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

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
 * @author Mark Paluch
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
			template.postForEntity(uri,
					new PromotionRequest(information.getTargetRepository(), properties.getStagingRepository()), String.class);
		} catch (HttpClientErrorException o_O) {
			handle(message -> logger.warn(information.getModule(), message), "Promotion failed!", o_O);
		}
	}

	public void verify() {

		URI verificationResource = properties.getServer().getVerificationResource();

		try {

			logger.log("Artifactory", "Verifying authentication using a GET call to %s.", verificationResource);

			template.getForEntity(verificationResource, String.class);

			logger.log("Artifactory", "Authentication verified!");

		} catch (HttpClientErrorException o_O) {
			handle(message -> logger.log("Artifactory Client", message), "Authentication verification failed!", o_O);
			throw new IllegalStateException("Authentication verification failed!");
		}
	}

	private void handle(Consumer<Object> logger, String message, HttpClientErrorException o_O) {

		try {

			logger.accept(message);

			Errors errors = new ObjectMapper().readValue(o_O.getResponseBodyAsByteArray(), Errors.class);
			errors.getErrors().forEach(logger);
			errors.getMessages().forEach(logger);

		} catch (IOException e) {
			o_O.addSuppressed(e);
			throw new RuntimeException(o_O.getResponseBodyAsString(), o_O);
		}
	}

	public void deleteArtifacts(DeploymentInformation information) {
		template.delete(properties.getServer().getDeleteBuildResource(information));
	}

	@Value
	static class PromotionRequest {
		String targetRepo, sourceRepo;
	}
}
