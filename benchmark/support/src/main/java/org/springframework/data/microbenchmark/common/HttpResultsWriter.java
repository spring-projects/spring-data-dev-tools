/*
 * Copyright 2018-2022 the original author or authors.
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
package org.springframework.data.microbenchmark.common;

import jmh.mbr.core.ResultsWriter;
import jmh.mbr.core.model.BenchmarkResults;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;

import lombok.SneakyThrows;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

/**
 * {@link ResultsWriterOld} implementation of {@link URLConnection}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@RequiredArgsConstructor
class HttpResultsWriter implements ResultsWriter {

	private final String url;


	@Override
	public void write(OutputFormat output, BenchmarkResults benchmarkResults) {

		if (CollectionUtils.isEmpty(benchmarkResults.getRawResults())) {
			return;
		}

		try {
			doWrite(benchmarkResults.getRawResults());
		} catch (IOException e) {
			output.println("Failed to write results: " + e);
		}
	}

	private void doWrite(Collection<RunResult> results) throws IOException {

		StandardEnvironment env = new StandardEnvironment();

		String projectVersion = env.getProperty("project.version", "unknown");
		String gitBranch = env.getProperty("git.branch", "unknown");
		String gitDirty = env.getProperty("git.dirty", "no");
		String gitCommitId = env.getProperty("git.commit.id", "unknown");

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setConnectTimeout((int) Duration.ofSeconds(1).toMillis());
		connection.setReadTimeout((int) Duration.ofSeconds(1).toMillis());
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");

		connection.setRequestProperty("Content-Type", "application/json");
		connection.addRequestProperty("X-Project-Version", projectVersion);
		connection.addRequestProperty("X-Git-Branch", gitBranch);
		connection.addRequestProperty("X-Git-Dirty", gitDirty);
		connection.addRequestProperty("X-Git-Commit-Id", gitCommitId);

		try (OutputStream output = connection.getOutputStream()) {
			output.write(jsonifyResults(results).getBytes(StandardCharsets.UTF_8));
		}

		if (connection.getResponseCode() >= 400) {
			throw new IllegalStateException(
					String.format("Status %d %s", connection.getResponseCode(), connection.getResponseMessage()));
		}
	}

	/**
	 * Convert {@link RunResult}s to JMH Json representation.
	 *
	 * @param results
	 * @return json string representation of results.
	 * @see org.openjdk.jmh.results.format.JSONResultFormat
	 */
	@SneakyThrows
	static String jsonifyResults(Collection<RunResult> results) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ResultFormatFactory.getInstance(ResultFormatType.JSON, new PrintStream(baos, true, "UTF-8")).writeOut(results);

		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}
}
