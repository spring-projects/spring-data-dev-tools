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
package org.springframework.data.release.git;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.release.issues.TicketReference;

/**
 * @author Mark Paluch
 */
@Getter
@ToString
class ParsedCommitMessage {

	private static final Pattern JIRA_TICKET = Pattern.compile("(?>\\[)?([A-Z]+[ ]?-[ ]?\\d+)(?>\\])?");
	private static final Pattern GITHUB_TICKET = Pattern.compile("((?>#|gh-)\\d+)");

	private static final Pattern GITHUB_CLOSE_SYNTAX = Pattern.compile(
			"(?>closes|closed|close|fixes|fixed|fix|resolves|resolved|resolve|see)[\\s:]*((?>#|gh-)\\d+)",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private static final Pattern GITHUB_PREFIX_SYNTAX = Pattern.compile("^(#\\d+)");

	private static final Pattern A_TICKET = Pattern
			.compile(String.format("(%s|%s)", JIRA_TICKET.pattern(), GITHUB_TICKET.pattern()));

	private static final Pattern ORIGINAL_PULL_REQUEST = Pattern
			.compile("Original (?>pull request|PR|pullrequest):(?>\\s+)?" + A_TICKET.pattern(), Pattern.CASE_INSENSITIVE);

	private static final Pattern RELATED_TICKET = Pattern.compile(
			"Related (?>tickets|ticket):(?>\\s+)?((" + A_TICKET.pattern() + "(?>[\\s,]*))+)", Pattern.CASE_INSENSITIVE);

	private final String summary;
	private final String body;

	private final TicketReference ticketReference;
	private final TicketReference pullRequestReference;
	private final List<TicketReference> relatedTickets;

	private ParsedCommitMessage(String summary, String body, TicketReference ticketReference,
			TicketReference pullRequestReference, List<TicketReference> relatedTickets) {
		this.summary = summary;
		this.body = body;
		this.ticketReference = ticketReference;
		this.pullRequestReference = pullRequestReference;
		this.relatedTickets = relatedTickets;
	}

	public static ParsedCommitMessage parse(String message) {

		int lineBreak = message.indexOf('\n');

		String summary = null;
		String body = null;
		TicketReference ticketReference = null;
		TicketReference pullRequestReference = null;

		if (lineBreak > -1) {
			summary = message.substring(0, lineBreak).trim();
			body = message.substring(lineBreak + 1).trim();
		} else {
			summary = message.trim();
		}

		// DATACASS-nnn - syntax
		Matcher jiraMatcher = JIRA_TICKET.matcher(summary);

		if (jiraMatcher.find()) {

			MatchResult mr = jiraMatcher.toMatchResult();

			// allow […] syntax and start of message syntax
			if (mr.start(1) < 2) {
				int summaryStart = findSummaryIndex(summary, mr.end(1));

				ticketReference = new TicketReference(jiraMatcher.group(1).toUpperCase(Locale.ROOT),
						summaryStart > -1 ? summary.substring(summaryStart) : summary, TicketReference.Style.Jira);
			}
		}

		// Closes (gh-nnn|#nnn) syntax
		Matcher gitHubMatcher = GITHUB_CLOSE_SYNTAX.matcher(message);

		// #nnn syntax
		Matcher gitHubPrefixMatcher = GITHUB_PREFIX_SYNTAX.matcher(summary);

		if (gitHubPrefixMatcher.find()) {

			MatchResult mr = gitHubPrefixMatcher.toMatchResult();
			if (mr.start(1) == 0) {
				int summaryStart = findSummaryIndex(summary, mr.end(1));

				ticketReference = new TicketReference(gitHubPrefixMatcher.group(1).toUpperCase(Locale.ROOT),
						summaryStart > -1 ? summary.substring(summaryStart) : summary, TicketReference.Style.GitHub);
			}

		} else {
			if (gitHubMatcher.find()) {
				ticketReference = new TicketReference(gitHubMatcher.group(1), summary, TicketReference.Style.GitHub);
			}
		}

		List<TicketReference> relatedTickets = new ArrayList<>();
		if (body != null) {
			Matcher relatedTicketsMatcher = RELATED_TICKET.matcher(body);

			if (relatedTicketsMatcher.find()) {

				String ticketIds[] = relatedTicketsMatcher.group(1).split(",");

				for (String ticketId : ticketIds) {
					extractTicket(ticketId).ifPresent(relatedTickets::add);
				}
			}

			while (gitHubMatcher.find()) {
				extractTicket(gitHubMatcher.group(1)).ifPresent(relatedTickets::add);
			}
		}

		if (body != null) {

			Matcher prMatcher = ORIGINAL_PULL_REQUEST.matcher(body);

			if (prMatcher.find()) {

				Optional<TicketReference> pullRequest = extractTicket(prMatcher.group(1));

				if (pullRequest.isPresent()) {
					pullRequestReference = pullRequest.get();
				}

				if (ticketReference == null && pullRequestReference != null) {
					ticketReference = pullRequestReference;
					pullRequestReference = null;
				}
			}
		}

		return new ParsedCommitMessage(summary, body, ticketReference, pullRequestReference, relatedTickets);
	}

	protected static Optional<TicketReference> extractTicket(String ticketId) {

		if (JIRA_TICKET.matcher(ticketId.trim()).matches()) {
			return Optional.of(new TicketReference(ticketId.trim(), null, TicketReference.Style.Jira));
		}

		if (GITHUB_TICKET.matcher(ticketId.trim()).matches()) {
			return Optional.of(new TicketReference(ticketId.trim(), null, TicketReference.Style.GitHub));
		}

		return Optional.empty();
	}

	private static int findSummaryIndex(String summary, int startAt) {

		int dash = summary.indexOf("- ", startAt);

		if (dash > -1) {
			return dash + 2;
		}

		int space = summary.indexOf(" ", startAt);

		if (space > -1) {
			return space + 1;
		}

		return -1;
	}

}
