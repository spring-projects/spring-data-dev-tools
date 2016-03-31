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

import lombok.Data;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.release.utils.HttpBasicCredentials;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriTemplate;

/**
 * @author Oliver Gierke
 */
@Data
@Component
@ConfigurationProperties(prefix = "deployment")
public class DeploymentProperties {

	/**
	 * The Artifactory host.
	 */
	private Server server;

	/**
	 * The deployer's username.
	 */
	private String username;

	private String apiKey;

	/**
	 * The deployer's password.
	 */
	private String password;

	/**
	 * The repository to deploy the artifacts to.
	 */
	private String stagingRepository;

	private String repositoryPrefix = "";

	private Gpg gpg;

	public String getStagingRepository() {
		return repositoryPrefix.concat(stagingRepository);
	}

	/**
	 * Returns the URI of the staging repository.
	 * 
	 * @return
	 */
	public String getStagingRepositoryUrl() {
		return server.getUri().toString().concat("/").concat(stagingRepository);
	}

	public HttpBasicCredentials getCredentials() {
		return new HttpBasicCredentials(username, password);
	}

	@Data
	public static class Server {

		private static final String PROMOTION_RESOURCE = "/api/build/promote/{buildName}/{buildNumber}";
		private static final String DELETE_BUILD_RESOURCE = "/api/build/{buildName}?buildNumbers={buildNumber}&artifacts=1";
		private static final String VERIFICATION_RESOURCE = "/api/storage/test-libs-staging-local";

		private String uri;

		/**
		 * Returns the URI to the resource that a promotion can be triggered at.
		 * 
		 * @param information must not be {@literal null}.
		 * @return
		 */
		public URI getPromotionResource(DeploymentInformation information) {

			Assert.notNull(information, "DeploymentInformation must not be null!");

			return new UriTemplate(uri.concat(PROMOTION_RESOURCE)).expand(information.getBuildInfoParameters());
		}

		public URI getDeleteBuildResource(DeploymentInformation information) {

			return new UriTemplate(uri.concat(DELETE_BUILD_RESOURCE)).expand(information.getBuildInfoParameters());
		}

		public URI getVerificationResource() {
			return URI.create(uri.concat(VERIFICATION_RESOURCE));
		}
	}

	@Data
	public static class Gpg {
		private String keyname, password, executable;
	}
}
