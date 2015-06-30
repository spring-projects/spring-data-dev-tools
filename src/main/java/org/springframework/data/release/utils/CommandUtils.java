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
package org.springframework.data.release.utils;

import java.util.concurrent.Future;

import org.springframework.data.release.io.CommandResult;

/**
 * @author Oliver Gierke
 */
public class CommandUtils {

	public static CommandResult getCommandResult(Future<CommandResult> future) throws Exception {

		CommandResult result = future.get();

		if (result.hasError()) {
			throw new CommandException(result);
		}

		return result;
	}

	public static class CommandException extends RuntimeException {

		private final CommandResult result;

		public CommandException(CommandResult result) {

			super(result.getException());
			this.result = result;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Throwable#getMessage()
		 */
		@Override
		public String getMessage() {
			return String.format("Command execution failed: %s.", result);
		}
	}
}
