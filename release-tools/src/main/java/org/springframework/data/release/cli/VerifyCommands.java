/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.release.cli;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.build.BuildOperations;
import org.springframework.data.release.deployment.DeploymentOperations;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.issues.github.GitHub;
import org.springframework.data.release.sagan.SaganClient;
import org.springframework.data.release.utils.Logger;
import org.springframework.shell.core.annotation.CliCommand;

/**
 * Commands to verify a correct Release Tools Setup.
 *
 * @author Mark Paluch
 */
@CliComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class VerifyCommands extends TimedCommand {

	@NonNull GitOperations git;
	@NonNull GitHub github;
	@NonNull DeploymentOperations deployment;
	@NonNull BuildOperations build;
	@NonNull SaganClient saganClient;
	@NonNull Logger logger;

	@CliCommand("verify")
	public void verifyReleaseTools() {

		// Git checkout build
		git.verify();

		// Maven interaction
		build.verify();

		// Artifactory verification
		deployment.verifyAuthentication();

		// GitHub verification
		github.verifyAuthentication();

		// Sagan Verification
		saganClient.verifyAuthentication();

		logger.log("Verify", "All settings are verified. You can ship a release now.");
	}

}
