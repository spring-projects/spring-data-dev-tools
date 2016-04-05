/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.data.release.issues.jira;

import lombok.NonNull;
import lombok.Value;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * Value object to represent a list of {@link JiraComponent}s.
 * 
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@Value(staticConstructor = "of")
class JiraComponents implements Iterable<JiraComponent> {

	@NonNull Collection<JiraComponent> jiraComponents;

	/**
	 * Try to find a component by its {@code name}.
	 * 
	 * @param name
	 * @return
	 */
	public Optional<JiraComponent> findComponent(String name) {
		return jiraComponents.stream().filter(jiraComponent -> jiraComponent.hasName(name)).findFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<JiraComponent> iterator() {
		return jiraComponents.iterator();
	}
}
