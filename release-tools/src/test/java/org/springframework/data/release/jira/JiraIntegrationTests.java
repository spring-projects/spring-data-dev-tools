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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.ProjectKey;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Integration Tests for {@link Jira} using a local {@link WireMockRule} server.
 * 
 * @author Mark Paluch
 */
public class JiraIntegrationTests extends AbstractIntegrationTests {

	public static final Credentials CREDENTIALS = new Credentials("dummy", "dummy");
	@Rule public WireMockRule mockService = new WireMockRule(
			wireMockConfig().port(8888).fileSource(new ClasspathFileSource("integration/jira")));

	@Rule public ExpectedException expectedException = ExpectedException.none();

	@Autowired JiraConnector jira;

	@Before
	public void before() throws Exception {
		jira.reset();
	}

	/**
	 * @see #5
	 */
	@Test
	public void findResolvedTicketsByTicketIds() throws Exception {

		prepareSearchAndReturn("findResolvedTicketsByTicketIds.json");

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("DATAREDIS-1", "DATAJPA-1"));
		assertThat(tickets, hasSize(2));
	}

	/**
	 * @see #5
	 */
	@Test
	public void ignoresUnknownTicketsByTicketId() throws Exception {

		prepareSearchAndReturn("ignoresUnknownTicketsByTicketId.json");

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

		prepareSearchAndReturn("getReleaseTicketForReturnsTheReleaseTicket.json");

		Ticket releaseTicket = jira.getReleaseTicketFor(ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST"));
		assertThat(releaseTicket.getId(), is(Matchers.equalTo("DATAREST-782")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void noReleaseTicketFound() throws Exception {

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("Did not find a release ticket for Spring Data REST 2.5 RC1");

		prepareSearchAndReturn("noReleaseTicketFound.json");

		jira.getReleaseTicketFor(ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST"));

		fail("Missing IllegalStateException");
	}

	/**
	 * @see #5
	 */
	@Test
	public void getReleaseVersion() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		prepareProjectVersionsAndReturn("getReleaseVersion.json", moduleIteration.getProjectKey());

		Optional<JiraReleaseVersion> optional = jira.findJiraReleaseVersion(moduleIteration);

		assertThat(optional.isPresent(), is(true));
		assertThat(optional.get().getName(), is(Matchers.equalTo("2.5 RC1 (Hopper)")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseVersionShouldCreateAVersion() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		prepareProjectVersionsAndReturn("noReleaseVersionFound.json", moduleIteration.getProjectKey());
		preparePostVersionAndReturn("createReleaseVersionShouldCreateAVersion.json");

		jira.createReleaseVersion(moduleIteration, CREDENTIALS);

		verify(postRequestedFor(urlPathMatching("/rest/api/2/version")).withRequestBody(
				equalToJson("{\"name\":\"2.5 RC1 (Hopper)\",\"project\":\"DATAREST\",\"description\":\"Hopper RC1\"}")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseVersionShouldFindExistingReleaseVersion() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		prepareProjectVersionsAndReturn("getReleaseVersion.json", moduleIteration.getProjectKey());

		jira.createReleaseVersion(moduleIteration, CREDENTIALS);

		verify(0, postRequestedFor(urlPathMatching("/rest/api/2/version")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldCreateReleaseTicket() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		prepareProjectVersionsAndReturn("getReleaseVersion.json", moduleIteration.getProjectKey());
		prepareProjectComponentsAndReturn("getProjectComponents.json", moduleIteration.getProjectKey());
		prepareSearchAndReturn("noReleaseTicketFound.json");
		prepareCreateIssueAndReturn("issueCreated.json");

		jira.createReleaseTicket(moduleIteration, CREDENTIALS);

		verify(postRequestedFor(urlPathMatching("/rest/api/2/issue"))
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

		prepareProjectVersionsAndReturn("getReleaseVersion.json", moduleIteration.getProjectKey());
		prepareProjectComponentsAndReturn("noProjectComponentsFound.json", moduleIteration.getProjectKey());
		prepareSearchAndReturn("noReleaseTicketFound.json");
		prepareCreateIssueAndReturn("issueCreated.json");

		jira.createReleaseTicket(moduleIteration, CREDENTIALS);

		verify(postRequestedFor(urlPathMatching("/rest/api/2/issue"))
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

		prepareSearchAndReturn("noReleaseTicketFound.json");
		prepareProjectVersionsAndReturn("noReleaseVersionFound.json", moduleIteration.getProjectKey());

		jira.createReleaseTicket(moduleIteration, CREDENTIALS);

		fail("Missing IllegalStateException");
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldFindExistingTicket() throws Exception {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "REST");

		prepareProjectVersionsAndReturn("getReleaseVersion.json", moduleIteration.getProjectKey());
		prepareProjectComponentsAndReturn("getProjectComponents.json", moduleIteration.getProjectKey());
		prepareSearchAndReturn("getReleaseTicketForReturnsTheReleaseTicket.json");

		jira.createReleaseTicket(moduleIteration, CREDENTIALS);

		verify(0, postRequestedFor(urlPathMatching("/rest/api/2/issue")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void assignTicketToMe() throws Exception {

		mockService.stubFor(post(urlPathMatching("/rest/api/2/issue/DATAREDIS-99999")).//
				willReturn(aResponse().withStatus(204)));

		jira.assignTicketToMe(new Ticket("DATAREDIS-99999", "", null), CREDENTIALS);

		verify(postRequestedFor(urlPathMatching("/rest/api/2/issue/DATAREDIS-99999"))
				.withRequestBody(equalToJson("{\"fields\":{\"assignee\":{\"name\":\"dummy\"}}}")));
	}

	private void prepareSearchAndReturn(String fromClassPath) {
		mockService.stubFor(get(urlPathMatching("/rest/api/2/search")).//
				willReturn(json(fromClassPath)));
	}

	private void prepareProjectVersionsAndReturn(String fromClassPath, ProjectKey projectKey) {
		mockService.stubFor(get(urlPathMatching("/rest/api/2/project/" + projectKey.getKey() + "/version")).//
				willReturn(json(fromClassPath)));
	}

	private void prepareProjectComponentsAndReturn(String fromClassPath, ProjectKey projectKey) {
		mockService.stubFor(get(urlPathMatching("/rest/api/2/project/" + projectKey.getKey() + "/components")).//
				willReturn(json(fromClassPath)));
	}

	private void prepareCreateIssueAndReturn(String fromClassPath) {
		mockService.stubFor(post(urlPathMatching("/rest/api/2/issue")).//
				willReturn(json(fromClassPath)));
	}

	private void preparePostVersionAndReturn(String fromClassPath) {
		mockService.stubFor(post(urlPathMatching("/rest/api/2/version")).//
				willReturn(json(fromClassPath)));
	}

	private ResponseDefinitionBuilder json(String fromClassPathFile) {
		return aResponse().//
				withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).//
				withBodyFile(fromClassPathFile);

	}

}
