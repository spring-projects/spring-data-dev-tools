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
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.bson.Document;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * MongoDB specific {@link ResultsWriter} implementation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Roman Puchkovskiy
 */
@RequiredArgsConstructor
class MongoResultsWriter implements ResultsWriter {

	private final String uri;

	@Override
	public void write(OutputFormat output, BenchmarkResults benchmarkResults) {

		if (CollectionUtils.isEmpty(benchmarkResults.getRawResults())) {
			return;
		}

		try {
			doWrite(benchmarkResults.getRawResults());
		} catch (ParseException | RuntimeException e) {
			output.println("Failed to write results: " + e.toString());
		}
	}

	private void doWrite(Collection<RunResult> results) throws ParseException {

		Date now = new Date();
		StandardEnvironment env = new StandardEnvironment();

		String projectVersion = env.getProperty("project.version", "unknown");
		String gitBranch = env.getProperty("git.branch", "unknown");
		String gitDirty = env.getProperty("git.dirty", "no");
		String gitCommitId = env.getProperty("git.commit.id", "unknown");

		ConnectionString uri = new ConnectionString(this.uri);
		MongoClient client = MongoClients.create();

		String dbName = StringUtils.hasText(uri.getDatabase()) ? uri.getDatabase() : "spring-data-mongodb-benchmarks";
		MongoDatabase db = client.getDatabase(dbName);

		String resultsJson = HttpResultsWriter.jsonifyResults(results).trim();
		JSONArray array = (JSONArray) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(resultsJson);
		for (Object object : array) {
			JSONObject dbo = (JSONObject) object;

			String collectionName = extractClass(dbo.get("benchmark").toString());

			Document sink = new Document();
			sink.append("_version", projectVersion);
			sink.append("_branch", gitBranch);
			sink.append("_commit", gitCommitId);
			sink.append("_dirty", gitDirty);
			sink.append("_method", extractBenchmarkName(dbo.get("benchmark").toString()));
			sink.append("_date", now);
			sink.append("_snapshot", projectVersion.toLowerCase().contains("snapshot"));

			sink.putAll(dbo);

			db.getCollection(collectionName).insertOne(fixDocumentKeys(sink));
		}

		client.close();
	}

	/**
	 * Replace {@code .} by {@code ,}.
	 *
	 * @param doc
	 * @return
	 */
	private static Document fixDocumentKeys(Document doc) {

		Document sanitized = new Document();

		for (Object key : doc.keySet()) {

			Object value = doc.get(key);
			if (value instanceof Document) {
				value = fixDocumentKeys((Document) value);
			} else if (value instanceof Map) {
				value = fixDocumentKeys(new Document((Map<String, Object>) value));
			}

			if (key instanceof String) {

				String newKey = (String) key;
				if (newKey.contains(".")) {
					newKey = newKey.replace('.', ',');
				}

				sanitized.put(newKey, value);
			} else {
				sanitized.put(ObjectUtils.nullSafeToString(key).replace('.', ','), value);
			}
		}

		return sanitized;
	}

	private static String extractClass(String source) {

		String tmp = source.substring(0, source.lastIndexOf('.'));
		return tmp.substring(tmp.lastIndexOf(".") + 1);
	}

	private static String extractBenchmarkName(String source) {
		return source.substring(source.lastIndexOf(".") + 1);
	}

}
