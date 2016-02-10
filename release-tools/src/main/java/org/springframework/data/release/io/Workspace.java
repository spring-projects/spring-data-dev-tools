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

import static org.springframework.data.release.utils.StreamUtils.*;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.release.model.Project;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Abstraction of the workspace that is used to work with the {@link Project}'s repositories, execute builds, etc.
 * 
 * @author Oliver Gierke
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class Workspace {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private final IoProperties ioProperties;
	private final ResourcePatternResolver resolver;

	/**
	 * Returns the current working directory.
	 * 
	 * @return
	 */
	public File getWorkingDirectory() {
		return ioProperties.getWorkDir();
	}

	/**
	 * Cleans up the working directory by removing all files and folders in it.
	 * 
	 * @throws IOException
	 */
	public void cleanup() throws IOException {

		Path workingDirPath = getWorkingDirectory().toPath();

		Files.walkFileTree(workingDirPath, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

				if (!workingDirPath.equals(dir)) {
					Files.delete(dir);
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Returns the directory for the given {@link Project}.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public File getProjectDirectory(Project project) {

		Assert.notNull(project, "Project must not be null!");
		return new File(getWorkingDirectory(), project.getName());
	}

	/**
	 * Returns whether the project directory for the given project already exists.
	 * 
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public boolean hasProjectDirectory(Project project) {

		Assert.notNull(project, "Project must not be null!");
		return getProjectDirectory(project).exists();
	}

	/**
	 * Returns a file with the given name relative to the working directory for the given {@link Project}.
	 * 
	 * @param name must not be {@literal null} or empty.
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public File getFile(String name, Project project) {

		Assert.hasText(name, "Filename must not be null or empty!");
		Assert.notNull(project, "Project must not be null!");

		return new File(getProjectDirectory(project), name);
	}

	public Stream<File> getFiles(String pattern, Project project) {

		File projectDirectory = getProjectDirectory(project);
		String patternToLookup = String.format("file:%s/%s", projectDirectory.getAbsolutePath(), pattern);

		try {
			return Arrays.stream(resolver.getResources(patternToLookup)).map(wrap(Resource::getFile));
		} catch (IOException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	public boolean processFiles(String pattern, Project project, LineCallback callback) {
		return false;
	}

	public boolean processFile(String filename, Project project, LineCallback callback) {

		File file = getFile(filename, project);

		if (!file.exists()) {
			return false;
		}

		StringBuilder builder = new StringBuilder();

		try (Scanner scanner = new Scanner(file)) {

			long number = 0;

			while (scanner.hasNextLine()) {

				String result = callback.doWith(scanner.nextLine(), number++);

				if (result != null) {
					builder.append(result).append("\n");
				}
			}

			writeContentToFile(filename, project, builder.toString());

		} catch (Exception o_O) {
			throw new RuntimeException(o_O);
		}

		return true;
	}

	private void writeContentToFile(String name, Project project, String content) throws IOException {

		File file = getFile(name, project);
		com.google.common.io.Files.write(content, file, UTF_8);
	}

	/**
	 * Initializes the working directory and creates the folders if necessary.
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	public void setUp() throws IOException {

		Path path = getWorkingDirectory().toPath();

		if (!java.nio.file.Files.exists(path)) {
			java.nio.file.Files.createDirectories(path);
		}
	}

	public interface LineCallback {
		String doWith(String line, long number);
	}
}
