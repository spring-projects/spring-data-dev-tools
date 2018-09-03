/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.microbenchmark.common;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.openjdk.jmh.results.RunResult;

/**
 * Elasticsearch specific {@link ResultsWriter} implementation.
 * 
 * @author Christoph Strobl
 * @since 2.1
 */
public class ElasticResultsWriter implements ResultsWriter {

	private final String uri;

	public ElasticResultsWriter(String uri) {
		this.uri = uri;
	}

	@Override
	public void write(Collection<RunResult> results) {

		List<IndexRequest> computedResults = results.stream() //
				.map(ResultsWriter::asMap) //
				.map(ElasticResultsWriter::createIndexRequest) //
				.collect(Collectors.toList());

		RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(extractHttpHost(uri)));

		computedResults.forEach(it -> {
			try {
				client.index(it, RequestOptions.DEFAULT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static IndexRequest createIndexRequest(Map<String, Object> source) {

		String index = "jmh_" + source.get("project_name");
		IndexRequest request = new IndexRequest(index, "doc");
		request.source(source);
		return request;
	}

	HttpHost extractHttpHost(String uri) {

		String parsableUri = uri;
		if (uri.startsWith("elastic")) {
			parsableUri = uri.replaceFirst("^elastic", "http");
		}

		try {

			URL url = new URL(parsableUri);
			return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

		} catch (MalformedURLException e) {
			return new HttpHost("localhost", 9200);
		}
	}
}
