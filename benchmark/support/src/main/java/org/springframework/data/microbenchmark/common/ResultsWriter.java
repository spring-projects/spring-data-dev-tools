/*
 * Copyright 2018 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * @author Christoph Strobl
 * @since 2.0
 */
interface ResultsWriter {

	/**
	 * Write the {@link RunResult}s.
	 *
	 * @param results can be {@literal null}.
	 */
	void write(Collection<RunResult> results);

	/**
	 * Get the uri specific {@link ResultsWriter}.
	 *
	 * @param uri must not be {@literal null}.
	 * @return
	 */
	static ResultsWriter forUri(String uri) {

		switch (uri.charAt(0)) {
			case 'm':
				return new MongoResultsWriter(uri);
			case 'e':
				return new ElasticResultsWriter(uri);
			default:
				return new HttpResultsWriter(uri);
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

	/**
	 * Convert a single {@link RunResult} to a generic JMH representation of the JMH Json format and enhance it with
	 * additional {@link Metadata meta information}.
	 *
	 * @param result
	 * @return json string representation of results.
	 * @see org.openjdk.jmh.results.format.JSONResultFormat
	 */
	@SneakyThrows
	static Map<String, Object> asMap(RunResult result) {

		Metadata metadata = new Metadata(new Date(), result);

		Map<String, Object> mapped = new LinkedHashMap<>(
				new ObjectMapper().readValue(jsonifyResults(Collections.singleton(result)), Map[].class)[0]);

		mapped.putAll(metadata.asMap());
		return mapped;
	}

	/**
	 * Convert a single {@link RunResult} to a generic JSON representation.
	 *
	 * @param result
	 * @return json string representation of results.
	 * @see org.openjdk.jmh.results.format.JSONResultFormat
	 */
	@SneakyThrows
	static String asJson(RunResult result) {

		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.setDateFormat(new StdDateFormat());

		return mapper.writeValueAsString(asMap(result));
	}

	/**
	 * Meta information read from the actual {@link RunResult} and {@link org.springframework.core.env.Environment}. The
	 * data computed here helps creating time series data based on analytic system friendly meta information such as the
	 * project name and version, git commit information and so on.
	 * 
	 * @since 2.1
	 * @author Christoph Strobl
	 */
	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	class Metadata {

		Date date;
		String projectName;
		String projectVersion;
		String gitCommitId;
		String benchmarkGroup;
		String benchmarkName;
		String os;

		Metadata(Date date, RunResult runResult) {

			this.date = date;

			Environment env = new StandardEnvironment();
			this.projectName = env.getProperty("project.name", "unknown");
			this.projectVersion = env.getProperty("project.version", "unknown");
			this.gitCommitId = env.getProperty("git.commit.id", "unknown");
			this.os = env.getProperty("os.name", "unknown");
			this.benchmarkGroup = extractBenchmarkGroup(runResult);
			this.benchmarkName = extractBenchmarkName(runResult);
		}

		public Map<String, Object> asMap() {

			Map<String, Object> metadata = new LinkedHashMap<>();

			metadata.put("date", date);
			metadata.put("project_name", projectName);
			metadata.put("project_version", projectVersion);
			metadata.put("snapshot", projectVersion.toLowerCase().contains("snapshot"));
			metadata.put("git_commit", gitCommitId);
			metadata.put("benchmark_group", benchmarkGroup);
			metadata.put("benchmark_name", benchmarkName);
			metadata.put("operating_system", os);

			return metadata;
		}

		private static String extractBenchmarkName(RunResult result) {

			String source = result.getParams().getBenchmark();
			return source.substring(source.lastIndexOf(".") + 1);
		}

		private static String extractBenchmarkGroup(RunResult result) {

			String source = result.getParams().getBenchmark();
			String tmp = source.substring(0, source.lastIndexOf('.'));
			return tmp.substring(tmp.lastIndexOf(".") + 1);
		}
	}
}
