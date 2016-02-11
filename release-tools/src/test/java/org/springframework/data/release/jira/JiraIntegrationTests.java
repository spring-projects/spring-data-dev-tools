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

package org.springframework.data.release.jira;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.model.Projects;

/**
 * @author Mark Paluch
 */
public class JiraIntegrationTests extends AbstractIntegrationTests {

	@Autowired JiraConnector jira;

	@Test
	public void findResolvedTicketsByTicketIds() throws Exception {

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("DATAREDIS-1", "DATAJPA-1"));
		assertThat(tickets, hasSize(2));
	}

	@Test
	public void ignoresUnknownTicketsByTicketId() throws Exception {

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("XYZ-1", "UNKOWN-1"));
		assertThat(tickets, hasSize(0));
	}

	@Test
	public void emptyResultWithEmptyTicketIds() throws Exception {

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList());
		assertThat(tickets, hasSize(0));
	}
}
