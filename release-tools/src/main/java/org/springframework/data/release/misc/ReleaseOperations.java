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
package org.springframework.data.release.misc;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.Logger;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Component
@RequiredArgsConstructor
public class ReleaseOperations {

	private final Workspace workspace;
	private final Logger logger;

	public void updateResources(TrainIteration iteration) {

		iteration.stream().forEach(module -> {

			boolean processed = workspace.processFile("src/main/resources/notice.txt", module.getProject(),
					(line, number) -> Optional.of(number != 0 ? line : module.toString()));

			if (processed) {
				logger.log(module, "Updated notice.txt.");
			}
		});
	}
}
