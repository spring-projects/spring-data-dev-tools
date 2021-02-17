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

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.TicketStatus;

/**
 * @author Mark Paluch
 */
class CommitUnitTests {

	@Test
	void shouldRenderCommitMessage() {

		assertThat(
				new Commit(new Ticket("1234", "Hello", Mockito.mock(TicketStatus.class)), "Summary", Optional.empty()))
						.hasToString("Summary.\n" + "\n" + "See 1234");
	}

	@Test
	void shouldRenderCommitMessageWithDetail() {

		assertThat(
				new Commit(new Ticket("1234", "Hello", Mockito.mock(TicketStatus.class)), "Summary",
						Optional.of("detail")))
								.hasToString("Summary.\n" + "\n" + "detail\n" + "\n" + "See 1234");
	}
}
