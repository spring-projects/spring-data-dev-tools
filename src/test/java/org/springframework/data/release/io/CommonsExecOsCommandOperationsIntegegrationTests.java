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
package org.springframework.data.release.io;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;

/**
 * @author Oliver Gierke
 */
public class CommonsExecOsCommandOperationsIntegegrationTests extends AbstractIntegrationTests {

	@Autowired OsCommandOperations operations;

	@Test
	public void testname() throws Exception {

		CommandResult result = operations
				.executeCommand("git clone --progress https://github.com/spring-projects/spring-data-build").get();

		if (result.hasError()) {
			System.out.println(result.getStatus());
			System.out.println(result.getException().getMessage());
		} else {
			System.out.println(result.getOutput());
		}

		assertThat(result.hasError(), is(false));

	}
}
