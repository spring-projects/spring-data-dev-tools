/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.release.infra;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;

import org.springframework.data.release.CliComponent;
import org.springframework.data.release.TimedCommand;
import org.springframework.data.release.git.GitOperations;
import org.springframework.data.release.io.Workspace;
import org.springframework.data.release.issues.IssueTracker;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.TicketOperations;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.utils.ExecutionUtils;
import org.springframework.data.release.utils.Logger;
import org.springframework.data.util.Streamable;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.AntPathMatcher;

/**
 * @author Mark Paluch
 */
@CliComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LicenseHeaderCommands extends TimedCommand {

	GitOperations git;

	Workspace workspace;

	Executor executor;

	Logger logger;

	TicketOperations tickets;

	List<String> filePatterns = Arrays.asList("pom.xml", "**/*.java", "**/*.kt", "**/*.adoc");

	/**
	 * Process all files matching {@link #filePatterns} and update the Apache license header year range, extending to
	 * {@code year}. Rewrites single-year and year-range formats.
	 *
	 * @param iteration
	 * @param year
	 */
	@CliCommand(value = "update license-headers")
	public void updateLicenseHeaders(@CliOption(key = "", mandatory = true) TrainIteration iteration,
			@CliOption(key = "year", mandatory = true) int year,
			@CliOption(key = "project", mandatory = false) String projectName) {

		git.prepare(iteration);

		Streamable<ModuleIteration> modules = iteration;

		if (projectName != null) {
			Project project = Projects.requiredByName(projectName);
			modules = modules.filter(it -> it.getProject().equals(project));
		}

		ExecutionUtils.run(executor, modules, module -> {

			String summary = String.format("Extend license header copyright years to %d", year);

			int updated = replaceInFiles(module.getProject(), content -> {

				String contentToUse = content;

				contentToUse = contentToUse.replaceAll("(C) ([\\d]{4})-([\\d]{4})", "(C) $1-" + year);

				contentToUse = contentToUse.replaceAll("Copyright ([\\d]{4}) the original author or authors",
						"Copyright $1-" + year + " the original author or authors");

				contentToUse = contentToUse.replaceAll("Copyright ([\\d]{4})-([\\d]{4}) the original author or authors",
						"Copyright $1-" + year + " the original author or authors");

				return contentToUse;
			});

			if (updated > 0) {
				commitAndPushWithTicket(module, summary);
			}
		});
	}

	private void commitAndPushWithTicket(ModuleIteration module, String ticketSummary) throws InterruptedException {

		Ticket ticket = tickets.getOrCreateTicketsWithSummary(module, IssueTracker.TicketType.Task, ticketSummary);
		git.commit(module, ticket, ticketSummary, Optional.empty(), true);

		try {
			git.push(module);
		} catch (Exception e) {
			logger.warn(module, e);
		}

		TimeUnit.SECONDS.sleep(1);

		tickets.closeTicket(module, ticket);
	}

	/**
	 * Replace content in files by applying {@link Function contentFunction} and return the number of updated files.
	 *
	 * @param project
	 * @param contentFunction
	 * @return
	 */
	private int replaceInFiles(Project project, Function<String, String> contentFunction) {

		File projectDirectory = workspace.getProjectDirectory(project);
		IOFileFilter fileFilter = new AntPathFileFilter(projectDirectory, filePatterns);

		int files = 0;
		int modified = 0;
		Iterator<File> fileIterator = FileUtils.iterateFiles(projectDirectory, fileFilter,
				new NotFileFilter(new NameFileFilter(".git")));

		while (fileIterator.hasNext()) {

			File file = fileIterator.next();
			files++;

			try {
				if (doReplace(file, contentFunction)) {
					modified++;
				}
			} catch (IOException e) {
				throw new IllegalStateException(String.format("Cannot modify contents of %s", file), e);
			}
		}

		logger.log(project, "Found %s files, updated %s files", files, modified);

		return modified;
	}

	private boolean doReplace(File file, Function<String, String> modifyFunction) throws IOException {

		String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		String modified = modifyFunction.apply(content);

		if (!content.equals(modified)) {

			FileUtils.write(file, modified, StandardCharsets.UTF_8);
			return true;
		}

		return false;
	}

	private static class AntPathFileFilter extends AbstractFileFilter {

		private final URI projectDirectory;
		private final List<String> filePatterns;

		public AntPathFileFilter(File basePath, List<String> filePatterns) {
			this.projectDirectory = basePath.toURI();
			this.filePatterns = filePatterns;
		}

		@Override
		public boolean accept(File file) {

			String relativePath = projectDirectory.relativize(file.toURI()).getPath();

			AntPathMatcher matcher = new AntPathMatcher();
			for (String pattern : filePatterns) {

				if (matcher.match(pattern, relativePath)) {
					return true;
				}
			}

			return false;
		}
	}
}
