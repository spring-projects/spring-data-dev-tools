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
package org.springframework.data.release.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Oliver Gierke
 */
public class Projects {

	public static final Project COMMONS, BUILD, REST, JPA, MONGO_DB, NEO4J, SOLR, COUCHBASE, CASSANDRA, ELASTICSEARCH,
			REDIS, GEMFIRE;
	public static final List<Project> PROJECTS;

	static {

		BUILD = new Project("DATABUILD", "Build", Tracker.GITHUB, Collections.emptyList(), Collections.emptyList());
		COMMONS = new Project("DATACMNS", "Commons", Arrays.asList(BUILD));
		JPA = new Project("DATAJPA", "JPA", Arrays.asList(COMMONS));
		MONGO_DB = new Project("DATAMONGO", "MongoDB", Arrays.asList(COMMONS),
				Arrays.asList("spring-data-mongodb-cross-store", "spring-data-mongodb-log4j"));
		NEO4J = new Project("DATAGRAPH", "Neo4j", Arrays.asList(COMMONS));
		SOLR = new Project("DATASOLR", "Solr", Arrays.asList(COMMONS));
		COUCHBASE = new Project("DATACOUCH", "Couchbase", Arrays.asList(COMMONS));
		CASSANDRA = new Project("DATACASS", "Cassandra", Arrays.asList(COMMONS), Arrays.asList("spring-cql"));
		ELASTICSEARCH = new Project("DATAES", "Elasticsearch", Arrays.asList(COMMONS));
		REDIS = new Project("DATAREDIS", "Redis", Collections.emptyList());
		GEMFIRE = new Project("SGF", "Gemfire", Arrays.asList(COMMONS));

		REST = new Project("DATAREST", "REST", Arrays.asList(COMMONS, JPA, MONGO_DB, NEO4J, GEMFIRE, SOLR, CASSANDRA),
				Arrays.asList("spring-data-rest-core", "spring-data-rest-core"));

		PROJECTS = Arrays.asList(BUILD, COMMONS, JPA, MONGO_DB, NEO4J, SOLR, COUCHBASE, CASSANDRA, ELASTICSEARCH, REDIS,
				GEMFIRE, REST);
	}
}
