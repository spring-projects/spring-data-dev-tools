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
package org.springframework.data.release.jira;

import java.util.Iterator;
import java.util.List;

import lombok.Data;

/**
 * @author Oliver Gierke
 */
@Data
class JiraIssues implements Iterable<JiraIssue> {

	private int startAt;
	private int maxResults;
	private int total;

	private List<JiraIssue> issues;

	public int getNextStartAt() {
		return startAt + issues.size();
	}

	public boolean hasMoreResults() {
		return startAt + issues.size() < total;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<JiraIssue> iterator() {
		return issues.iterator();
	}
}
