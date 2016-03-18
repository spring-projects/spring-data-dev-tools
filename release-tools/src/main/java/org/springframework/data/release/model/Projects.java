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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * @author Oliver Gierke
 */
public class Projects {

	public static final Project COMMONS, BUILD, REST, JPA, MONGO_DB, NEO4J, SOLR, COUCHBASE, CASSANDRA, ELASTICSEARCH,
			REDIS, GEMFIRE, KEY_VALUE, ENVERS;
	public static final List<Project> PROJECTS;

	static {

		BUILD = new Project("DATABUILD", "Build", Tracker.GITHUB, Collections.emptyList(),
				ArtifactCoordinates.forGroupId("org.springframework.data.build")
						.artifacts("spring-data-build-parent", "spring-data-build-resources")
						.artifact(ArtifactCoordinate.of("org.springframework.data", "spring-data-releasetrain")));
		COMMONS = new Project("DATACMNS", "Commons", Arrays.asList(BUILD));
		JPA = new Project("DATAJPA", "JPA", Arrays.asList(COMMONS));
		MONGO_DB = new Project("DATAMONGO", "MongoDB", Arrays.asList(COMMONS),
				ArtifactCoordinates.NONE.artifacts("spring-data-mongodb-cross-store", "spring-data-mongodb-log4j"));
		NEO4J = new Project("DATAGRAPH", "Neo4j", Arrays.asList(COMMONS));
		SOLR = new Project("DATASOLR", "Solr", Arrays.asList(COMMONS));
		COUCHBASE = new Project("DATACOUCH", "Couchbase", Arrays.asList(COMMONS));
		CASSANDRA = new Project("DATACASS", "Cassandra", Arrays.asList(COMMONS),
				ArtifactCoordinates.NONE.artifacts("spring-cql"));
		ELASTICSEARCH = new Project("DATAES", "Elasticsearch", Arrays.asList(COMMONS));
		KEY_VALUE = new Project("DATAKV", "KeyValue", Arrays.asList(COMMONS));
		REDIS = new Project("DATAREDIS", "Redis", Arrays.asList(COMMONS, KEY_VALUE));
		GEMFIRE = new Project("SGF", "Gemfire", Arrays.asList(COMMONS));

		REST = new Project("DATAREST", "REST",
				Arrays.asList(COMMONS, JPA, MONGO_DB, NEO4J, GEMFIRE, SOLR, CASSANDRA, KEY_VALUE), ArtifactCoordinates.NONE
						.artifacts("spring-data-rest-core", "spring-data-rest-core", "spring-data-rest-hal-browser"));

		ENVERS = new Project("DATAENV", "Envers", Tracker.GITHUB, Arrays.asList(JPA, COMMONS), ArtifactCoordinates.NONE);

		List<Project> projects = Arrays.asList(BUILD, COMMONS, JPA, MONGO_DB, NEO4J, SOLR, COUCHBASE, CASSANDRA,
				ELASTICSEARCH, REDIS, GEMFIRE, REST, KEY_VALUE, ENVERS);

		DefaultDirectedGraph<Project, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

		projects.forEach(project -> {

			graph.addVertex(project);

			project.getDependencies().forEach(dependency -> {
				graph.addVertex(dependency);
				graph.addEdge(project, dependency);
			});
		});

		Iterator<Project> iterator = new TopologicalOrderIterator<>(graph);
		List<Project> intermediate = new ArrayList<>(projects.size());

		while (iterator.hasNext()) {
			intermediate.add(iterator.next());
		}

		Collections.reverse(intermediate);

		PROJECTS = Collections.unmodifiableList(intermediate);
	}

	public static Project byName(String name) {

		return PROJECTS.stream().//
				filter(project -> project.getName().equalsIgnoreCase(name)).//
				findFirst().orElseThrow(() -> new IllegalArgumentException("No project named %s available!"));
	}
}
