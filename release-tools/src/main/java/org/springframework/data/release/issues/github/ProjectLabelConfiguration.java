/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.release.issues.github;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;

/**
 * {@link Project-specifc} Label Configuration.
 *
 * @author Mark Paluch
 */
public class ProjectLabelConfiguration {

	static final Map<Project, LabelConfiguration> labelConfigurations = new HashMap<>();

	static {

		LabelConfiguration commonLabels = LabelConfiguration.commonLabels();

		LabelConfiguration coreMappingRepository = LabelConfiguration.of(config -> {

			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "mapping", "Mapping and conversion infrastructure");
			config.register(LabelFactories.IN_LABEL, "repository", "Repositories abstraction");
		});

		labelConfigurations.put(Projects.BOM, LabelConfiguration.empty().mergeWith(config -> {

			config.register(commonLabels.getRequiredLabel(LabelFactories.TYPE_LABEL, "task"));
		}));

		labelConfigurations.put(Projects.BUILD, LabelConfiguration.empty().mergeWith(config -> {

			config.register(commonLabels.getRequiredLabel(LabelFactories.TYPE_LABEL, "dependency-upgrade"));
			config.register(commonLabels.getRequiredLabel(LabelFactories.TYPE_LABEL, "task"));
		}));

		labelConfigurations.put(Projects.CASSANDRA, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "repository", "Repositories abstraction");
			config.register(LabelFactories.IN_LABEL, "kotlin", "Kotlin support");
		}));

		labelConfigurations.put(Projects.COMMONS, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "web", "Integration with Spring MVC");
			config.register(LabelFactories.IN_LABEL, "kotlin", "Kotlin support");
		}));

		labelConfigurations.put(Projects.COUCHBASE, commonLabels.mergeWith(coreMappingRepository));

		labelConfigurations.put(Projects.ELASTICSEARCH, commonLabels.mergeWith(coreMappingRepository));

		labelConfigurations.put(Projects.ENVERS, commonLabels.mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
		}));

		labelConfigurations.put(Projects.GEODE, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "api", "Issues with API");
			config.register(LabelFactories.IN_LABEL, "aeq", "AsyncEventQueue");
			config.register(LabelFactories.IN_LABEL, "build", "Build issues");
			config.register(LabelFactories.IN_LABEL, "configuration", "Issues with configuration");
			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "documentation", "Issues in documentation");
			config.register(LabelFactories.IN_LABEL, "cq", "Continuous Queries");
			config.register(LabelFactories.IN_LABEL, "functions", "Functions");
			config.register(LabelFactories.IN_LABEL, "infrastructure", "Environment issues");
			config.register(LabelFactories.IN_LABEL, "query", "OQL Query & Indexing");
			config.register(LabelFactories.IN_LABEL, "repository", "Repositories abstraction");
			config.register(LabelFactories.IN_LABEL, "search", "Apache Lucene Integration");
			config.register(LabelFactories.IN_LABEL, "security", "Security issues");
			config.register(LabelFactories.IN_LABEL, "serialization", "Serialization issues");
			config.register(LabelFactories.IN_LABEL, "transactions", "Transaction issues");
			config.register(LabelFactories.IN_LABEL, "wan", "WAN");
		}));

		labelConfigurations.put(Projects.GEMFIRE, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "api", "Issues with API");
			config.register(LabelFactories.IN_LABEL, "aeq", "AsyncEventQueue");
			config.register(LabelFactories.IN_LABEL, "build", "Build issues");
			config.register(LabelFactories.IN_LABEL, "configuration", "Issues with configuration");
			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "documentation", "Issues in documentation");
			config.register(LabelFactories.IN_LABEL, "cq", "Continuous Queries");
			config.register(LabelFactories.IN_LABEL, "functions", "Functions");
			config.register(LabelFactories.IN_LABEL, "infrastructure", "Environment issues");
			config.register(LabelFactories.IN_LABEL, "query", "OQL Query & Indexing");
			config.register(LabelFactories.IN_LABEL, "repository", "Repositories abstraction");
			config.register(LabelFactories.IN_LABEL, "search", "Apache Lucene Integration");
			config.register(LabelFactories.IN_LABEL, "security", "Security issues");
			config.register(LabelFactories.IN_LABEL, "serialization", "Serialization issues");
			config.register(LabelFactories.IN_LABEL, "transactions", "Transaction issues");
			config.register(LabelFactories.IN_LABEL, "wan", "WAN");
		}));

		labelConfigurations.put(Projects.JDBC, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "statement-builder", "SQL Statement builder API");
			config.register(LabelFactories.IN_LABEL, "relational", "Relational module");
			config.register(LabelFactories.IN_LABEL, "kotlin", "Kotlin support");
		}));

		labelConfigurations.put(Projects.JPA, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "query-parser", "Everything related to parsing JPQL or SQL");
			config.register(LabelFactories.IN_LABEL, "querydsl", "Querydsl integration");
		}));

		labelConfigurations.put(Projects.KEY_VALUE, commonLabels.mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "map", "Map repositories/Map adapter");
			config.register(LabelFactories.IN_LABEL, "repository", "Repositories abstraction");
		}));

		labelConfigurations.put(Projects.LDAP, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "core", "Issues in core support");
			config.register(LabelFactories.IN_LABEL, "repository", "Repositories abstraction");
		}));

		labelConfigurations.put(Projects.NEO4J, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "ogm", "Object Graph Mapping (Legacy)");
		}));

		labelConfigurations.put(Projects.MONGO_DB, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "aggregation-framework", "Aggregation framework support");
			config.register(LabelFactories.IN_LABEL, "gridfs", "GridFS support");
			config.register(LabelFactories.IN_LABEL, "kotlin", "Kotlin support");
		}));

		labelConfigurations.put(Projects.R2DBC, commonLabels.mergeWith(coreMappingRepository));

		labelConfigurations.put(Projects.REST, commonLabels.mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "api-documentation", "");
			config.register(LabelFactories.IN_LABEL, "content-negotiation", "");
			config.register(LabelFactories.IN_LABEL, "query-parser", "Everything related to parsing JPQL or SQL");
		}));

		labelConfigurations.put(Projects.REDIS, commonLabels.mergeWith(coreMappingRepository).mergeWith(config -> {

			config.register(LabelFactories.IN_LABEL, "cache", "RedisCache and CacheManager");
			config.register(LabelFactories.IN_LABEL, "lettuce", "Lettuce driver");
			config.register(LabelFactories.IN_LABEL, "jedis", "Jedis driver");
			config.register(LabelFactories.IN_LABEL, "kotlin", "Kotlin support");
		}));

		labelConfigurations.put(Projects.SOLR, commonLabels.mergeWith(coreMappingRepository));

		verify();
	}

	/**
	 * Retrieve the {@link LabelConfiguration} for a {@link Project}.
	 *
	 * @param project must not be {@literal null}.
	 * @return
	 */
	public static LabelConfiguration forProject(Project project) {
		return labelConfigurations.get(project);
	}

	static void verify() {

		for (Project project : Projects.all()) {

			if (!ProjectLabelConfiguration.labelConfigurations.containsKey(project)) {
				throw new IllegalStateException(String.format("No LabelConfiguration for %s", project.getName()));
			}
		}
	}

}
