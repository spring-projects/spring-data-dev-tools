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

import java.util.Date;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import org.springframework.data.release.model.ArtifactVersion;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.shell.support.util.OsUtils;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
public class Changelog {

	private final ModuleIteration module;
	private final Tickets tickets;

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		ArtifactVersion version = ArtifactVersion.from(module);

		String headline = String.format("Changes in version %s (%s)", version,
				new DateFormatter("YYYY-MM-dd").print(new Date(), Locale.US));

		StringBuilder builder = new StringBuilder(headline).append(OsUtils.LINE_SEPARATOR);

		for (int i = 0; i < headline.length(); i++) {
			builder.append("-");
		}

		builder.append(OsUtils.LINE_SEPARATOR);

		for (Ticket ticket : tickets) {

			String summary = ticket.getSummary();

			builder.append("* ").append(ticket.getId()).append(" - ").append(summary);

			if (!summary.endsWith(".")) {
				builder.append(".");
			}

			builder.append(OsUtils.LINE_SEPARATOR);
		}

		return builder.toString();
	}
}
