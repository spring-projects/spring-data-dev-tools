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
package org.springframework.data.release.git;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link ParsedCommitMessage}.
 *
 * @author Mark Paluch
 */
class ParsedCommitMessageUnitTests {

	@Test
	void shouldParsePlainCommit() {

		ParsedCommitMessage commit = ParsedCommitMessage.parse("hello world");
		assertThat(commit.getSummary()).isEqualTo("hello world");
		assertThat(commit.getBody()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "DATAFOO-123 - Hello World.", "DATAFOO-123 Hello World.", "[DATAFOO-123] - Hello World.",
			"DATAFOO - 123 - Hello World." })
	void shouldParseOneLinerCommitWithJiraTicket(String commitMessage) {

		ParsedCommitMessage commit = ParsedCommitMessage.parse(commitMessage);

		assertThat(commit.getSummary()).endsWith("Hello World.");
		assertThat(commit.getTicketReference().getId()).isEqualTo("DATAFOO-123");
	}

	@Test
	void considersMultipleTicketsAsRelatedTickets() {

		ParsedCommitMessage commit = ParsedCommitMessage
				.parse("Polishing\n\nAdd tests for write and delete.\n\nCloses gh-503\nCloses gh-511");

		assertThat(commit.getTicketReference().getId()).isEqualTo("#503");
		assertThat(commit.getRelatedTickets()).hasSize(1);
		assertThat(commit.getRelatedTickets().get(0).getId()).isEqualTo("#511");
	}

	@ParameterizedTest
	@ValueSource(strings = { "close", "closes", "closed", "fix", "fixes", "fixed", "resolve", "resolves", "resolved" })
	void considersResolvesSyntax(String prefix) {

		ParsedCommitMessage commit = ParsedCommitMessage.parse("Polishing\n\n" + prefix + " gh-586.");

		assertThat(commit.getTicketReference().getId()).isEqualTo("#586");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Incorporate review feedback\n\nSee gh-574." })
	void shouldParseCommitWithSeeTicket(String commitMessage) {

		ParsedCommitMessage commit = ParsedCommitMessage.parse(commitMessage);

		assertThat(commit.getSummary()).isEqualTo("Incorporate review feedback");
		assertThat(commit.getTicketReference().getId()).isEqualTo("#574");
	}

	@Test
	void shouldParseCommitWithRelatedTickets() {

		ParsedCommitMessage commit = ParsedCommitMessage
				.parse("DATAFOO-123 - Hello World.\n Related tickets: DATACMNS-1438, DATACMNS-1461, DATACMNS-1609.");

		assertThat(commit.getTicketReference().getId()).isEqualTo("DATAFOO-123");
		assertThat(commit.getRelatedTickets()).hasSize(3);
	}

	@ParameterizedTest
	@ValueSource(strings = { "DATAFOO-456 - Hello World.\n Original pull request: #415.",
			"DATAFOO-456 - Hello World.\n Original pr: #415." })
	void shouldParseCommitWithPullRequest(String commitMessage) {

		ParsedCommitMessage commit = ParsedCommitMessage.parse(commitMessage);

		assertThat(commit.getTicketReference().getId()).isEqualTo("DATAFOO-456");
		assertThat(commit.getPullRequestReference()).isNotNull();
		assertThat(commit.getPullRequestReference().getId()).isEqualTo("#415");
	}

}
