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
import java.util.List;
import java.util.Optional;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * @author Oliver Gierke
 */
public class Projects {

	public static final Project COMMONS, BUILD, REST, JPA, MONGO_DB, NEO4J, SOLR, COUCHBASE, CASSANDRA, ELASTICSEARCH,
			REDIS, GEMFIRE, KEY_VALUE, ENVERS, LDAP, GEODE;
	public static final List<Project> PROJECTS;

	static {

		BUILD = new Project("DATABUILD", "Build", Tracker.GITHUB) //
				.withAdditionalArtifacts(ArtifactCoordinates.forGroupId("org.springframework.data.build")
						.artifacts("spring-data-build-parent", "spring-data-build-resources")
						.and(ArtifactCoordinate.of("org.springframework.data", "spring-data-releasetrain")));

		COMMONS = new Project("DATACMNS", "Commons").withDependencies(BUILD);

		JPA = new Project("DATAJPA", "JPA").withDependencies(COMMONS);

		MONGO_DB = new Project("DATAMONGO", "MongoDB") //
				.withDependencies(COMMONS) //
				.withAdditionalArtifacts(
						ArtifactCoordinates.SPRING_DATA.artifacts("spring-data-mongodb-cross-store", "spring-data-mongodb-log4j"));

		NEO4J = new Project("DATAGRAPH", "Neo4j").withDependencies(COMMONS);

		SOLR = new Project("DATASOLR", "Solr") //
				.withDependencies(COMMONS) //
				.withFullName("Spring Data for Apache Solr");

		COUCHBASE = new Project("DATACOUCH", "Couchbase").withDependencies(COMMONS);

		CASSANDRA = new Project("DATACASS", "Cassandra")//
				.withDependencies(COMMONS) //
				.withAdditionalArtifacts(ArtifactCoordinates.SPRING_DATA.artifacts("spring-cql"))
				.withFullName("Spring Data for Apache Cassandra");

		ELASTICSEARCH = new Project("DATAES", "Elasticsearch").withDependencies(COMMONS);

		KEY_VALUE = new Project("DATAKV", "KeyValue").withDependencies(COMMONS);

		REDIS = new Project("DATAREDIS", "Redis").withDependencies(KEY_VALUE);

		GEMFIRE = new Project("SGF", "Gemfire")//
				.withDependencies(COMMONS)//
				.withSkipTests(true);

		GEODE = new Project("DATAGEODE", "Geode")//
				.withDependencies(COMMONS) //
				.withFullName("Spring Data for Apache Geode") //
				.withSkipTests(true);

		REST = new Project("DATAREST", "REST")//
				.withDependencies(JPA, MONGO_DB, NEO4J, GEMFIRE, SOLR, CASSANDRA, KEY_VALUE) //
				.withAdditionalArtifacts(ArtifactCoordinates.SPRING_DATA //
						.artifacts("spring-data-rest-core", "spring-data-rest-core", "spring-data-rest-hal-browser"));

		ENVERS = new Project("DATAENV", "Envers", Tracker.GITHUB).withDependencies(JPA);

		LDAP = new Project("DATALDAP", "LDAP").withDependencies(COMMONS);

		List<Project> projects = Arrays.asList(BUILD, COMMONS, JPA, MONGO_DB, NEO4J, SOLR, COUCHBASE, CASSANDRA,
				ELASTICSEARCH, REDIS, GEMFIRE, REST, KEY_VALUE, ENVERS, LDAP, GEODE);

		DefaultDirectedGraph<Project, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

		projects.forEach(project -> {

			graph.addVertex(project);

			project.getDependencies().forEach(dependency -> {
				graph.addVertex(dependency);
				graph.addEdge(project, dependency);
			});
		});

		List<Project> intermediate = new ArrayList<>(projects.size());
		new TopologicalOrderIterator<>(graph).forEachRemaining(it -> intermediate.add(it));

		Collections.reverse(intermediate);

		PROJECTS = Collections.unmodifiableList(intermediate);
	}

	public static Optional<Project> byName(String name) {

		return PROJECTS.stream().//
				filter(project -> project.getName().equalsIgnoreCase(name) || project.getKey().toString().equals(name)).//
				findFirst();
	}

	public static Project requiredByName(String name) {

		return byName(name).//
				orElseThrow(() -> new IllegalArgumentException(String.format("No project named %s available!", name)));
	}
}
