/*
 * Copyright 2014 the original author or authors.
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

import java.io.StringWriter;

import lombok.RequiredArgsConstructor;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CommandExecution {

	private final DefaultExecuteResultHandler resultHandler;
	private final StringWriter writer;

	private String output;

	public Exception getException() {
		return resultHandler.getException();
	}

	public int getExitValue() {
		return resultHandler.getExitValue();
	}

	public String waitAndGetOutput() {

		if (output != null) {
			return output;
		}

		try {
			waitForResult();
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		this.output = writer.toString();
		IOUtils.closeQuietly(writer);

		return output;
	}

	public void waitForResult() throws InterruptedException {
		resultHandler.waitFor();
	}
}
