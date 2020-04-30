/*
 * Copyright 2016-2020 the original author or authors.
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
package org.springframework.data.release.issues.jira;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.WireMockExtension;
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

/**
 * Integration Tests for {@link Jira} using a local {@link WireMockExtension} server.
 *
 * @author Mark Paluch
 */
class JiraConnectorIntegrationTests extends AbstractIntegrationTests {

	static final String CREATE_ISSUE_URI = "/rest/api/2/issue";
	static final String CREATE_VERSION_URI = "/rest/api/2/version";
	static final String UPDATE_VERSION_URI = "/rest/api/2/version/15475";
	static final String SEARCH_URI = "/rest/api/2/search";
	static final String PROJECT_VERSION_URI = "/rest/api/2/project/%s/version";
	static final String PROJECT_COMPONENTS_URI = "/rest/api/2/project/%s/components";
	static final ModuleIteration REST_HOPPER_RC1 = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

	@RegisterExtension WireMockExtension mockService = new WireMockExtension(
			wireMockConfig().port(8888).fileSource(new ClasspathFileSource("integration/jira")));

	@Autowired JiraConnector jira;
	@Autowired JiraProperties properties;

	@BeforeEach
	void before() {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(properties.getApiUrl()).build();
		assumeThat(uriComponents.getHost()).isEqualTo("localhost");

		properties.setUsername("dummy");
		jira.reset();
	}

	@Test // #5
	void findResolvedTicketsByTicketIds() {

		mockSearchWith("DATAREDIS-1andDATAJPA-1.json");

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("DATAREDIS-1", "DATAJPA-1"));
		assertThat(tickets).hasSize(2);
	}

	@Test // #5
	void ignoresUnknownTicketsByTicketId() {

		mockSearchWith("emptyTickets.json");

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList("XYZ-1", "UNKOWN-1"));
		assertThat(tickets).hasSize(0);
	}

	@Test // #5
	void emptyResultWithEmptyTicketIds() {

		Collection<Ticket> tickets = jira.findTickets(Projects.COMMONS, Arrays.asList());
		assertThat(tickets).hasSize(0);
	}

	@Test // #5
	void getReleaseTicketForReturnsTheReleaseTicket() {

		mockSearchWith("releaseTickets.json");

		Ticket releaseTicket = jira.getReleaseTicketFor(REST_HOPPER_RC1);
		assertThat(releaseTicket.getId()).isEqualTo("DATAREST-782");
	}

	@Test // #5
	void noReleaseTicketFound() {

		mockSearchWith("emptyTickets.json");

		assertThatIllegalArgumentException().isThrownBy(() -> jira.getReleaseTicketFor(REST_HOPPER_RC1))
				.withMessageContaining("Did not find a release ticket for Spring Data REST 2.5 RC1");
	}

	@Test // #5
	void getReleaseVersion() {

		mockGetProjectVersionsWith("releaseVersions.json", REST_HOPPER_RC1.getProjectKey());

		Optional<JiraReleaseVersion> optional = jira.findJiraReleaseVersion(REST_HOPPER_RC1);

		assertThat(optional.isPresent()).isTrue();
		assertThat(optional.get().getName()).isEqualTo("2.5 RC1 (Hopper)");
	}

	@Test // #5
	void createReleaseVersionShouldCreateAVersion() {

		mockGetProjectVersionsWith("emptyReleaseVersions.json", REST_HOPPER_RC1.getProjectKey());
		mockCreateVersionWith("versionCreated.json");

		jira.createReleaseVersion(REST_HOPPER_RC1);

		verify(postRequestedFor(urlPathMatching(CREATE_VERSION_URI)).withRequestBody(equalToJson(
				"{\"name\":\"2.5 RC1 (Hopper)\",\"project\":\"DATAREST\",\"description\":\"Hopper RC1\", \"released\":false, \"archived\":false}")));
	}

	@Test // #5
	void createReleaseVersionShouldFindExistingReleaseVersion() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());

		jira.createReleaseVersion(moduleIteration);

		verify(0, postRequestedFor(urlPathMatching(CREATE_VERSION_URI)));
	}

	@Test // #56
	void archiveReleaseVersionShouldArchiveReleaseVersion() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());
		mockService.stubFor(put(urlPathMatching(UPDATE_VERSION_URI)).//
				willReturn(json("versionCreated.json")));

		jira.archiveReleaseVersion(moduleIteration);

		verify(putRequestedFor(urlPathMatching(UPDATE_VERSION_URI))
				.withRequestBody(equalToJson("{\"id\":\"15475\",\"name\":\"2.5 RC1 (Hopper)\","
						+ "\"description\":\"Hopper RC1\", \"released\":true,\"archived\":true}")));
	}

	@Test // #5
	void createReleaseTicketShouldCreateReleaseTicket() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

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

	@Test // #5
	void createReleaseTicketShouldCreateReleaseTicketWithoutComponent() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

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

	@Test // #5
	void createReleaseTicketShouldFailWithNoReleaseVersion() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

		mockSearchWith("emptyTickets.json");
		mockGetProjectVersionsWith("emptyReleaseVersions.json", moduleIteration.getProjectKey());

		assertThatIllegalStateException().isThrownBy(() -> jira.createReleaseTicket(moduleIteration))
				.withMessageContaining("Did not find a release version for Spring Data REST 2.5 RC1");
	}

	@Test // #5
	void createReleaseTicketShouldFindExistingTicket() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());
		mockGetProjectComponentsWith("projectComponents.json", moduleIteration.getProjectKey());
		mockSearchWith("releaseTickets.json");

		jira.createReleaseTicket(moduleIteration);

		verify(0, postRequestedFor(urlPathMatching(CREATE_ISSUE_URI)));
	}

	/**
	 * @see #5, #54
	 */
	@Test
	void assignTicketToMe() {

		mockService.stubFor(get(urlPathMatching("/rest/api/2/issue/DATAREDIS-302")).//
				willReturn(json("existingTicket.json")));

		mockService.stubFor(put(urlPathMatching("/rest/api/2/issue/DATAREDIS-302")).//
				willReturn(aResponse().withStatus(204)));

		jira.assignTicketToMe(new Ticket("DATAREDIS-302", "", null));

		verify(putRequestedFor(urlPathMatching("/rest/api/2/issue/DATAREDIS-302")).withRequestBody(equalToJson(
				"{\"update\":{\"assignee\":[ {\"set\":{\"name\":\"dummy\"}} ] }, \"transition\":{}, \"fields\":{}}")));
	}

	/**
	 * @see #5, #53
	 */
	@Test
	void skipTicketAssignmentIfAssigned() {

		properties.setUsername("mp911de");

		mockService.stubFor(get(urlPathMatching("/rest/api/2/issue/DATACASS-302")).//
				willReturn(json("existingTicket.json")));

		mockService.stubFor(post(urlPathMatching("/rest/api/2/issue/DATACASS-302")).//
				willReturn(aResponse().withStatus(204)));

		jira.assignTicketToMe(new Ticket("DATACASS-302", "", null));

		verify(0, postRequestedFor(urlPathMatching("/rest/api/2/issue/DATACASS-302")));
	}

	@Test // #94
	void closeIterationShouldResolveReleaseTicket() {

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Projects.REST, Iteration.RC1);

		properties.setUsername("mp911de");

		mockGetProjectVersionsWith("releaseVersions.json", moduleIteration.getProjectKey());
		mockSearchWith("releaseTickets.json");

		mockService.stubFor(get(urlPathMatching("/rest/api/2/issue/DATAREST-782")).//
				willReturn(json("releaseTicket.json")));

		mockService.stubFor(post(urlPathMatching("/rest/api/2/issue/DATAREST-782/transitions")) //
				.willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(200)));

		jira.closeIteration(moduleIteration);

		verify(postRequestedFor(urlPathMatching("/rest/api/2/issue/DATAREST-782/transitions")).withRequestBody(
				equalToJson("{\"update\":{},\"transition\":{\"id\":5},\"fields\":{\"resolution\":{\"name\":\"Complete\"}}}")));
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
