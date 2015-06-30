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

import static org.springframework.data.release.model.Projects.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Oliver Gierke
 */
public class ReleaseTrains {

	public static final List<Train> TRAINS;
	public static final Train CODD, DIJKSTRA, EVANS, FOWLER;

	static {

		CODD = codd();
		DIJKSTRA = dijkstra();
		EVANS = DIJKSTRA.next("Evans", Transition.MINOR);
		FOWLER = EVANS.next("Fowler", Transition.MINOR);

		// Trains

		TRAINS = Arrays.asList(CODD, DIJKSTRA, EVANS, FOWLER);

		// Train names

		List<String> names = new ArrayList<>(TRAINS.size());

		for (Train train : TRAINS) {
			names.add(train.getName());
		}
	}

	private static Train codd() {

		Module build = new Module(BUILD, "1.3");
		Module commons = new Module(COMMONS, "1.7");
		Module jpa = new Module(JPA, "1.5");
		Module mongoDb = new Module(MONGO_DB, "1.4");
		Module neo4j = new Module(NEO4J, "3.0");
		Module solr = new Module(SOLR, "1.1");

		Module rest = new Module(REST, "2.0");

		return new Train("Codd", build, commons, jpa, mongoDb, neo4j, solr, rest);
	}

	private static Train dijkstra() {

		Module build = new Module(BUILD, "1.4");
		Module commons = new Module(COMMONS, "1.8");
		Module jpa = new Module(JPA, "1.6");
		Module mongoDb = new Module(MONGO_DB, "1.5");
		Module neo4j = new Module(NEO4J, "3.1");
		Module solr = new Module(SOLR, "1.2");
		Module couchbase = new Module(COUCHBASE, "1.1");
		Module cassandra = new Module(CASSANDRA, "1.0");
		Module elasticsearch = new Module(ELASTICSEARCH, "1.0", "M2");
		Module gemfire = new Module(GEMFIRE, "1.4");
		Module redis = new Module(REDIS, "1.3");

		Module rest = new Module(REST, "2.1");

		return new Train("Dijkstra", build, commons, jpa, mongoDb, neo4j, solr, couchbase, cassandra, elasticsearch,
				gemfire, redis, rest);
	}

	public static Train getTrainByName(String name) {

		for (Train train : TRAINS) {
			if (train.getName().equalsIgnoreCase(name)) {
				return train;
			}
		}

		return null;
	}

	public static Project getProjectByName(String name) {

		for (Project project : PROJECTS) {
			if (project.getName().equalsIgnoreCase(name)) {
				return project;
			}
		}

		return null;
	}
}
