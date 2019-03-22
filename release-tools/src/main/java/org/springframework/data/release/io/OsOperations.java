/*
 * Copyright 2011-2012 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import org.springframework.data.release.model.Project;

/**
 * Operations type to allow execution of native OS commands from the Spring Roo shell.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface OsOperations {

	/**
	 * Attempts the execution of a commands and delegates the output to the standard logger.
	 * 
	 * @param command the command to execute
	 * @throws IOException if an error occurs
	 */
	Future<CommandResult> executeCommand(String command) throws IOException;

	Future<CommandResult> executeCommand(String command, Project project) throws IOException;

	Future<CommandResult> executeWithOutput(String command, Project project) throws IOException;

	String executeForResult(String command, Project project) throws Exception;

	File getJavaHome();
}
