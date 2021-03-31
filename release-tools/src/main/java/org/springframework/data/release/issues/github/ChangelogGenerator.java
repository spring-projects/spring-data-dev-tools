/*
 * Copyright 2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Generates a changelog markdown file which includes bug fixes, enhancements and contributors for a given milestone.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
@Component
public class ChangelogGenerator {

	private static final Pattern ghUserMentionPattern = Pattern.compile("(^|[^\\w`])(@[\\w-]+)");

	private final Set<String> excludeLabels;

	private final Set<String> excludeContributors;

	private final String contributorsTitle;

	private final ChangelogSections sections;

	public ChangelogGenerator() {
		this.excludeLabels = new HashSet<>(Arrays.asList("type: task"));
		this.excludeContributors = Collections.emptySet();
		this.contributorsTitle = null;
		this.sections = new ChangelogSections();
	}

	/**
	 * Generates a file at the given path which includes bug fixes, enhancements and contributors for the given milestone.
	 *
	 * @param issues the issues to generate the changelog for
	 * @param sectionContentPostProcessor the postprocessor for a changelog section
	 */
	public String generate(List<GitHubReadIssue> issues,
			BiFunction<ChangelogSection, String, String> sectionContentPostProcessor) {
		return generateContent(issues, sectionContentPostProcessor);
	}

	private boolean isExcluded(GitHubReadIssue issue) {
		return issue.getLabels().stream().anyMatch(this::isExcluded);
	}

	private boolean isExcluded(Label label) {
		return this.excludeLabels.contains(label.getName());
	}

	private String generateContent(List<GitHubReadIssue> issues,
			BiFunction<ChangelogSection, String, String> sectionContentPostProcessor) {
		StringBuilder content = new StringBuilder();
		addSectionContent(content, this.sections.collate(issues), sectionContentPostProcessor);
		Set<GitHubUser> contributors = getContributors(issues);
		if (!contributors.isEmpty()) {
			addContributorsContent(content, contributors);
		}
		return content.toString();
	}

	private void addSectionContent(StringBuilder result, Map<ChangelogSection, List<GitHubReadIssue>> sectionIssues,
			BiFunction<ChangelogSection, String, String> sectionContentPostProcessor) {

		sectionIssues.forEach((section, issues) -> {

			issues.sort(Comparator.reverseOrder());

			StringBuilder content = new StringBuilder();

			content.append((content.length() != 0) ? String.format("%n") : "");
			content.append("## ").append(section).append(String.format("%n%n"));
			issues.stream().map(this::getFormattedIssue).forEach(content::append);

			result.append(sectionContentPostProcessor.apply(section, content.toString()));
		});
	}

	private String getFormattedIssue(GitHubReadIssue issue) {
		String title = issue.getTitle();
		title = ghUserMentionPattern.matcher(title).replaceAll("$1`$2`");
		return String.format("- %s %s%n", title, getLinkToIssue(issue));
	}

	private String getLinkToIssue(GitHubIssue issue) {
		return "[" + issue.getId() + "]" + "(" + issue.getUrl() + ")";
	}

	private Set<GitHubUser> getContributors(List<GitHubReadIssue> issues) {
		if (this.excludeContributors.contains("*")) {
			return Collections.emptySet();
		}
		return issues.stream().filter((issue) -> issue.getPullRequest() != null).map(GitHubReadIssue::getUser)
				.filter(this::isIncludedContributor).collect(Collectors.toSet());
	}

	private boolean isIncludedContributor(GitHubUser user) {
		return !this.excludeContributors.contains(user.getName());
	}

	private void addContributorsContent(StringBuilder content, Set<GitHubUser> contributors) {
		content.append(String.format("%n## "));
		content.append((this.contributorsTitle != null) ? this.contributorsTitle : ":heart: Contributors");
		content.append(String.format("%n%nWe'd like to thank all the contributors who worked on this release!%n%n"));
		contributors.stream().map(this::formatContributors).forEach(content::append);
	}

	private String formatContributors(GitHubUser c) {
		return String.format("- [@%s](%s)%n", c.getName(), c.getUrl());
	}

}
