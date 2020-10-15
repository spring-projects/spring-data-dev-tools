/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.io;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Christoph Strobl
 */
public class IoPropertiesUnitTests {

	@TempDir File tempDir;
	IoProperties properties;

	@BeforeEach
	void beforeEach() {
		properties = new IoProperties();
	}

	@Test // GH-152
	void setJavaHomeToDir() {

		properties.setJavaHome(tempDir.getPath());

		assertThat(properties.getJavaHome()).isNotNull().isDirectory();
	}

	@Test // GH-152
	void setJavaHomeIgnoresEmptyString() {

		properties.setJavaHome("");

		assertThat(properties.getJavaHome()).isNull();
	}

	@Test // GH-152
	void setJavaHomeIgnoresFile() throws IOException {

		File f = new File(tempDir, "java.txt");
		f.createNewFile();

		properties.setJavaHome(f.getPath());

		assertThat(properties.getJavaHome()).isNull();
	}

	@Test // GH-152
	void setJavaHomeIgnoresDirectoryThatDoesNotExist() throws IOException {

		properties.setJavaHome(new File(tempDir, "java-bin").getPath());

		assertThat(properties.getJavaHome()).isNull();
	}
}
