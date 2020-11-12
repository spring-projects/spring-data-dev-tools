/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.release.issues.github;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.release.git.GitProject;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.utils.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Methods to interact with GitHub labels.
 *
 * @author Mark Paluch
 */
@Component
public class GitHubLabels extends GitHubSupport {

	private static final String LABELS_URI = "/repos/spring-projects/{repoName}/labels";
	private static final String LABEL_URI = "/repos/spring-projects/{repoName}/labels/{label}";

	private static final ParameterizedTypeReference<List<Label>> LABELS_TYPE = new ParameterizedTypeReference<List<Label>>() {};

	private final Logger logger;

	public GitHubLabels(@Qualifier("tracker") RestTemplateBuilder templateBuilder, Logger logger,
			GitHubProperties properties) {

		super(createOperations(templateBuilder, properties));
		this.logger = logger;
	}

	/**
	 * Creates or updates GitHub labels for a {@link Project}. The actual {@link LabelConfiguration} is obtained from
	 * {@link ProjectLabelConfiguration}.
	 *
	 * @param project the project to process.
	 */
	public void createOrUpdateLabels(Project project) {

		logger.log(project, "Obtaining labels…");
		Map<String, Object> parameters = Collections.singletonMap("repoName", GitProject.of(project).getRepositoryName());

		List<Label> existsOnGitHub = getLabelsFromGitHub(parameters);
		LabelConfiguration configuration = ProjectLabelConfiguration.forProject(project);
		List<Label> newLabels = configuration.getNewLabels(existsOnGitHub);
		List<Label> existingLabels = configuration.getExistingLabels(existsOnGitHub);
		List<Label> additionalLabels = configuration.getAdditionalLabels(existsOnGitHub);

		if (!newLabels.isEmpty()) {

			logger.log(project, "Creating new labels (%d)…", newLabels.size());
			newLabels.forEach(it -> {
				operations.postForObject(LABELS_URI, it, JsonNode.class, parameters);
			});
		}

		newLabels.forEach(it -> {
			operations.postForObject(LABELS_URI, it, String.class, parameters);
		});

		existingLabels.stream().filter(it -> {

			Label existing = existsOnGitHub.get(existsOnGitHub.indexOf(it));

			return existing.requiresUpdate(it);
		}).forEach(it -> {

			logger.log(project, "Updating label %s…", it.getName());
			Map<String, Object> updateLabel = new HashMap<>(parameters);
			updateLabel.put("label", it.getName());

			operations.postForObject(LABEL_URI, it, JsonNode.class, updateLabel);
		});

		if (!additionalLabels.isEmpty()) {
			logger.log(project, "Found additional labels: %s",
					additionalLabels.stream().map(Label::getName).collect(Collectors.joining(", ")));
		}
	}

	protected List<Label> getLabelsFromGitHub(Map<String, Object> parameters) {

		List<Label> existsOnGitHub = new ArrayList<>();

		doWithPaging(LABELS_URI, HttpMethod.GET, parameters, new HttpEntity<>(new HttpHeaders()), LABELS_TYPE, labels -> {
			existsOnGitHub.addAll(labels);
			return true;
		});

		return existsOnGitHub;
	}

}
