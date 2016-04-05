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

package org.springframework.data.release.issues.github;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.release.AbstractIntegrationTests;
import org.springframework.data.release.issues.Ticket;
import org.springframework.data.release.issues.github.GitHub;
import org.springframework.data.release.issues.github.GitHubProperties;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ModuleIteration;
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
 * Integration Tests for {@link GitHub} using a local {@link WireMockRule} server.
 * 
 * @author Mark Paluch
 */
public class GitHubIssueTrackerIntegrationTests extends AbstractIntegrationTests {

	public static final String ISSUES_URI = "/repos/spring-projects/spring-data-build/issues";
	public static final String MILESTONES_URI = "/repos/spring-projects/spring-data-build/milestones";
	public static final ModuleIteration BUILD_HOPPER_RC1 = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1,
			"Build");

	@Rule public WireMockRule mockService = new WireMockRule(
			wireMockConfig().port(8888).fileSource(new ClasspathFileSource("integration/github")));

	@Rule public ExpectedException expectedException = ExpectedException.none();

	@Autowired GitHub github;
	@Autowired GitHubProperties properties;

	@Before
	public void before() throws Exception {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(properties.getApiUrl()).build();
		Assume.assumeThat(uriComponents.getHost(), is("localhost"));

		github.reset();

	}

	/**
	 * @see #5
	 */
	@Test
	public void findTicketsByTicketIds() throws Exception {

		mockGetIssueWith("issue.json", 233);

		Collection<Ticket> tickets = github.findTickets(Projects.BUILD, Arrays.asList("233"));
		assertThat(tickets, hasSize(1));
	}

	/**
	 * @see #5
	 */
	@Test
	public void ignoresUnknownTicketsByTicketId() throws Exception {

		Collection<Ticket> tickets = github.findTickets(Projects.BUILD, Arrays.asList("123"));
		assertThat(tickets, hasSize(0));
	}

	/**
	 * @see #5
	 */
	@Test
	public void emptyResultWithEmptyTicketIds() throws Exception {

		Collection<Ticket> tickets = github.findTickets(Projects.COMMONS, Arrays.asList());
		assertThat(tickets, hasSize(0));
	}

	/**
	 * @see #5
	 */
	@Test
	public void getReleaseTicketForReturnsTheReleaseTicket() throws Exception {

		mockGetMilestonesWith("milestones.json");
		mockGetIssuesWith("issues.json");

		Ticket releaseTicket = github.getReleaseTicketFor(BUILD_HOPPER_RC1);
		assertThat(releaseTicket.getId(), is(Matchers.equalTo("#233")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void noReleaseTicketFound() throws Exception {

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("No milestone for Spring Data Build found containing 1.8 RC1!");

		mockGetMilestonesWith("emptyMilestones.json");

		github.getReleaseTicketFor(BUILD_HOPPER_RC1);

		fail("Missing IllegalStateException");
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseVersionShouldCreateAVersion() throws Exception {

		mockGetMilestonesWith("emptyMilestones.json");
		mockCreateMilestoneWith("milestone.json");

		github.createReleaseVersion(BUILD_HOPPER_RC1);

		verify(postRequestedFor(urlPathMatching(MILESTONES_URI))
				.withRequestBody(equalToJson("{\"title\":\"1.8 RC1 (Hopper)\", \"description\":\"Hopper RC1\"}")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseVersionShouldFindExistingReleaseVersion() throws Exception {

		mockGetMilestonesWith("milestones.json");

		github.createReleaseVersion(BUILD_HOPPER_RC1);

		verify(0, postRequestedFor(urlPathMatching(MILESTONES_URI)));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldCreateReleaseTicket() throws Exception {

		mockGetMilestonesWith("milestones.json");
		mockGetIssuesWith("emptyIssues.json");
		mockCreateIssueWith("issue.json");

		github.createReleaseTicket(BUILD_HOPPER_RC1);

		verify(postRequestedFor(urlPathMatching(ISSUES_URI))
				.withRequestBody(equalToJson("{\"title\":\"Release 1.8 RC1 (Hopper)\",\"milestone\":45}")));
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldFailWithNoReleaseVersion() throws Exception {

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("No milestone for Spring Data Build found containing 1.8 RC1!");

		ModuleIteration moduleIteration = ReleaseTrains.HOPPER.getModuleIteration(Iteration.RC1, "Build");

		mockGetIssuesWith("emptyIssues.json");
		mockGetMilestonesWith("emptyMilestones.json");

		github.createReleaseTicket(moduleIteration);

		fail("Missing IllegalStateException");
	}

	/**
	 * @see #5
	 */
	@Test
	public void createReleaseTicketShouldFindExistingTicket() throws Exception {

		mockGetMilestonesWith("milestones.json");
		mockGetIssuesWith("issues.json");

		github.createReleaseTicket(BUILD_HOPPER_RC1);

		verify(0, postRequestedFor(urlPathMatching(MILESTONES_URI)));
	}

	private void mockGetIssueWith(String fromClassPath, int issueId) {
		mockService.stubFor(get(urlPathMatching(ISSUES_URI + "/" + issueId)).//
				willReturn(json(fromClassPath)));
	}

	private void mockGetIssuesWith(String fromClassPath) {
		mockService.stubFor(get(urlPathMatching(ISSUES_URI)).//
				willReturn(json(fromClassPath)));
	}

	private void mockCreateIssueWith(String fromClassPath) {
		mockService.stubFor(post(urlPathMatching(ISSUES_URI)).//
				willReturn(json(fromClassPath)));
	}

	private void mockGetMilestonesWith(String fromClassPath) {
		mockService.stubFor(get(urlPathMatching(MILESTONES_URI)).//
				willReturn(json(fromClassPath)));
	}

	private void mockCreateMilestoneWith(String fromClassPath) {
		mockService.stubFor(post(urlPathMatching(MILESTONES_URI)).//
				willReturn(json(fromClassPath)));
	}

	private ResponseDefinitionBuilder json(String fromClassPathFile) {
		return aResponse().//
				withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).//
				withBodyFile(fromClassPathFile);

	}
}
