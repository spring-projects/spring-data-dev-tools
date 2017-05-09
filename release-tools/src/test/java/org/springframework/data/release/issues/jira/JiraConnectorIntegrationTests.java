/*
 * Copyright 2016-2017 the original author or authors.
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ProjectKey;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Integration Tests for {@link Jira} using a local {@link WireMockRule} server.
 *
 * @author Mark Paluch
 */
public class JiraConnectorIntegrationTests extends AbstractIntegrationTests {

	public static final String CREATE_ISSUE_URI = "/rest/api/2/issue";
	public static final String CREATE_VERSION_URI = "/rest/api/2/version";
	public static final String SEARCH_URI = "/rest/api/2/search";
	public static final String PROJECT_VERSION_URI = "/rest/api/2/project/%s/version";
	public static final String PROJECT_COMPONENTS_URI = "/rest/api/2/project/%s/components";
	public static final ModuleIteration REST_HOPPER_RC1 = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

	@Rule public WireMockRule mockService = new WireMockRule(
			wireMockConfig().port(8888).fileSource(new ClasspathFileSource("integration/jira")));

	@Rule public ExpectedException expectedException = ExpectedException.none();

	@Autowired JiraConnector jira;
	@Autowired JiraProperties properties;

	@Before
	public void before() throws Exception {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(properties.getApiUrl()).build();
		Assume.assumeThat(uriComponents.getHost(), is("localhost"));

		properties.setUsername("dummy");
		jira.reset();
	}

	/**
	 * @see #5
	 */
	@Test
	public void findResolvedTicketsByTicketIds() throws Exception {

		mockSearchWith("DATAREDIS-1andDATAJPA-1.json");

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("DATAREDIS-1", "DATAJPA-1"));
		assertThat(tickets, hasSize(2));
	}

	/**
	 * @see #5
	 */
	@Test
	public void ignoresUnknownTicketsByTicketId() throws Exception {

		mockSearchWith("emptyTickets.json");

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("XYZ-1", "UNKOWN-1"));
		assertThat(tickets, hasSize(0));
	}

	/**
	 * @see #5
	 */
	@Test
	public void emptyResultWithEmptyTicketIds() throws Exception {

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList());
		assertThat(tickets, hasSize(0));
	}

	/**
	 * @see #5
	 */
	@Test
	public void getReleaseTicketForReturnsTheReleaseTicket() throws Exception {

		mockSearchWith("releaseTickets.json");

		Ticket releaseTicket = jira.getReleaseTicketFor(REST_HOPPER_RC1);
		assertThat(releaseTicket.getId(), is(Matchers.equalTo("DATAREST-782")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void noReleaseTicketFound() throws Exception {

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Did not find a release ticket for Spring Data REST 2.5 RC1");

		mockSearchWith("emptyTickets.json");

		jira.getReleaseTicketFor(REST_HOPPER_RC1);

		fail("Missing IllegalStateException");
	}

	/**
	 * @see #5
	 */
	@Test
	public void getReleaseVersion() throws Exception {

		mockGetProjectVersionsWith("releaseVersions.json", REST_HOPPER_RC1.getProjectKey());

		Optional<JiraReleaseVersion> optional = jira.findJiraReleaseVersion(REST_HOPPER_RC1);

		assertThat(optional.isPresent(), is(true));
		assertThat(optional.get().getName(), is(Matchers.equalTo("2.5 RC1 (Hopper)")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseVersionShouldCreateAVersion() throws Exception {

		mockGetProjectVersionsWith("emptyReleaseVersions.json", REST_HOPPER_RC1.getProjectKey());
		mockCreateVersionWith("versionCreated.json");

		jira.createReleaseVersion(REST_HOPPER_RC1);

		verify(postRequestedFor(urlPathMatching(CREATE_VERSION_URI)).withRequestBody(
				equalToJson("{\"name\":\"2.5 RC1 (Hopper)\",\"project\":\"DATAREST\",\"description\":\"Hopper RC1\"}")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseVersionShouldFindExistingReleaseVersion() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());

		jira.createReleaseVersion(moduleIteration);

		verify(0, postRequestedFor(urlPathMatching(CREATE_VERSION_URI)));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldCreateReleaseTicket() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());
		mockGetProjectComponentsWith("projectComponents.json", moduleIteration.getProjectKey());
		mockSearchWith("emptyTickets.json");
		prepareCreateIssueAndReturn("issueCreated.json");

		jira.createReleaseTicket(moduleIteration);

		verify(postRequestedFor(urlPathMatching(CREATE_ISSUE_URI))
				.withRequestBody(equalToJson("{\"fields\":{\"project\":{\"key\":\"DATAREST\"},"
						+ "\"issuetype\":{\"name\":\"Task\"},\"summary\":\"Release 2.5 RC1 (Hopper)\","
						+ "\"fixVersions\":[{\"name\":\"2.5 RC1 (Hopper)\"}],"
						+ "\"components\":[{\"name\":\"Infrastructure\"}]}}")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldCreateReleaseTicketWithoutComponent() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());
		mockGetProjectComponentsWith("emptyProjectComponents.json", moduleIteration.getProjectKey());
		mockSearchWith("emptyTickets.json");
		prepareCreateIssueAndReturn("issueCreated.json");

		jira.createReleaseTicket(moduleIteration);

		verify(postRequestedFor(urlPathMatching(CREATE_ISSUE_URI))
				.withRequestBody(equalToJson("{\"fields\":{\"project\":{\"key\":\"DATAREST\"},"
						+ "\"issuetype\":{\"name\":\"Task\"},\"summary\":\"Release 2.5 RC1 (Hopper)\","
						+ "\"fixVersions\":[{\"name\":\"2.5 RC1 (Hopper)\"}]}}")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldFailWithNoReleaseVersion() throws Exception {

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("Did not find a release version for Spring Data REST 2.5 RC1");

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		mockSearchWith("emptyTickets.json");
		mockGetProjectVersionsWith("emptyReleaseVersions.json", moduleIteration.getProjectKey());

		jira.createReleaseTicket(moduleIteration);

		fail("Missing IllegalStateException");
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldFindExistingTicket() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());
		mockGetProjectComponentsWith("projectComponents.json", moduleIteration.getProjectKey());
		mockSearchWith("releaseTickets.json");

		jira.createReleaseTicket(moduleIteration);

		verify(0, postRequestedFor(urlPathMatching(CREATE_ISSUE_URI)));
	}

	/**
	 * @see #5
	 */
	@Test
	public void assignTicketToMe() {

		mockService.stubFor(get(urlPathMatching("/rest/api/2/issue/DATAREDIS-302")).//
				willReturn(json("existingTicket.json")));

		mockService.stubFor(post(urlPathMatching("/rest/api/2/issue/DATAREDIS-302")).//
				willReturn(aResponse().withStatus(204)));

		jira.assignTicketToMe(new Ticket("DATAREDIS-99999", "", null));

		verify(postRequestedFor(urlPathMatching("/rest/api/2/issue/DATAREDIS-302"))
				.withRequestBody(equalToJson("{\"fields\":{\"assignee\":{\"name\":\"dummy\"}}}")));
	}

	/**
	 * @see #5, #53
	 */
	@Test
	public void skipTicketAssignmentIfAssigned() {

		properties.setUsername("mp911de");

		mockService.stubFor(get(urlPathMatching("/rest/api/2/issue/DATACASS-302")).//
				willReturn(json("existingTicket.json")));

		mockService.stubFor(post(urlPathMatching("/rest/api/2/issue/DATACASS-302")).//
				willReturn(aResponse().withStatus(204)));

		jira.assignTicketToMe(new Ticket("DATACASS-302", "", null));

		verify(0, postRequestedFor(urlPathMatching("/rest/api/2/issue/DATACASS-302")));
	}

	private void mockSearchWith(String fromClassPath) {
		mockService.stubFor(get(urlPathMatching(SEARCH_URI)).//
				willReturn(json(fromClassPath)));
	}

	private void mockGetProjectVersionsWith(String fromClassPath, ProjectKey projectKey) {
		mockService.stubFor(get(urlPathMatching(String.format(PROJECT_VERSION_URI, projectKey))).//
				willReturn(json(fromClassPath)));
	}

	private void mockGetProjectComponentsWith(String fromClassPath, ProjectKey projectKey) {
		mockService.stubFor(get(urlPathMatching(String.format(PROJECT_COMPONENTS_URI, projectKey))).//
				willReturn(json(fromClassPath)));
	}

	private void prepareCreateIssueAndReturn(String fromClassPath) {
		mockService.stubFor(post(urlPathMatching(CREATE_ISSUE_URI)).//
				willReturn(json(fromClassPath)));
	}

	private void mockCreateVersionWith(String fromClassPath) {
		mockService.stubFor(post(urlPathMatching(CREATE_VERSION_URI)).//
				willReturn(json(fromClassPath)));
	}

	private ResponseDefinitionBuilder json(String fromClassPathFile) {
		return aResponse().//
				withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).//
				withBodyFile(fromClassPathFile);

	}

}
