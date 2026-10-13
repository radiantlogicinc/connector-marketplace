package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.JOB_POSTINGS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.utility.TestUtil;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for job posting-related functionality.
 *
 * <p>Uses a per-class test instance lifecycle to allow shared state
 * and non-static setup across test methods.</p>
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
class JobPostingIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * Path used to identify the job posting resource in API requests.
   */
  private static final String JOB_POSTING_ID_PATH = "/1234";

  @BeforeEach
  void setUp() {

    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY))
        .thenReturn(Collections.singletonList("jobPosting"));
    when(schema.getSchemaObject(any()))
        .thenReturn(Optional.of(TestUtil.getSchemaObjectForJobPosting()));
    wireMockServer.stubFor(post(
        urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
        .willReturn(aResponse()
            .withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  /**
   * Tests for scenarios where job postings exist.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * job postings are available.
   */
  @Nested
  @DisplayName("Given job postings exist")
  class GivenJobPostingsExist {

    @BeforeEach
    void setup() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("jobPostings.json"))));
    }

    @Test
    @DisplayName("Should return all job postings")
    void shouldReturnAllJobPostings() throws Exception {
      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(baseRequest());
      assertThat(count(dataProvider)).isEqualTo(3);
    }
  }

  /**
   * Tests for scenarios where a job posting exists in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * a job posting is available.
   */
  @Nested
  @DisplayName("Given a job posting exists in the system")
  class GivenJobPostingExists {

    @BeforeEach
    void setupJobPosting() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + JOB_POSTING_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("jobPosting.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the job posting")
    void shouldReturnJobPosting() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleJobPostingRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the job posting API returns an invalid response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the job posting API response is invalid.
   */
  @Nested
  @DisplayName("Given job posting API returns invalid response")
  class GivenInvalidJobPostingResponse {

    @BeforeEach
    void setupInvalidResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + JOB_POSTING_ID_PATH))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("invalid_response.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleJobPostingRequest()).next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where the API rejects a request as a bad request.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API returns a bad request response.
   */
  @Nested
  @DisplayName("Given API rejects request as bad request")
  class GivenBadRequest {

    @BeforeEach
    void setupBadRequest() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + JOB_POSTING_ID_PATH))
              .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleJobPostingRequest()).next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where authentication fails.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * authentication is unsuccessful.
   */
  @Nested
  @DisplayName("Given authentication fails")
  class GivenAuthenticationFails {

    @BeforeEach
    void setupAuthFailure() {
      wireMockServer.stubFor(
          post(urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .willReturn(aResponse().withStatus(401)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleJobPostingRequest()).next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where the job postings API supports pagination.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * pagination is enabled for job postings API responses.
   */
  @Nested
  @DisplayName("Given job postings API supports pagination")
  class GivenPaginationIsEnabled {

    @BeforeEach
    void setupPagination() {

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + "?limit=100&offset=0"))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("paginated_jobPostings.json"))));

      wireMockServer.stubFor(
          get(urlEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + "?limit=100&offset=100"))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("jobPostings.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return all job postings across pages")
    void shouldReturnAllJobPostingsAcrossPages() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(paginatedRequest());

      assertThat(count(dataProvider)).isEqualTo(6);
    }
  }

  /**
   * Tests for scenarios where the API rate limits requests.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API returns a rate limit response without a header.
   */
  @Nested
  @DisplayName("Given API rate limits requests")
  class GivenRateLimitWithoutHeader {

    @BeforeEach
    void setupRetryScenario() {

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + JOB_POSTING_ID_PATH))
              .inScenario("jobPosting-retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + JOB_POSTING_ID_PATH))
              .inScenario("jobPosting-retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("jobPosting.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should retry and succeed")
    void shouldRetryAndSucceed() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleJobPostingRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the API rate limits requests with a Retry-After header.
   *
   * <p>This nested test class groups test cases that verify retry behavior when
   * the API provides a Retry-After header in the rate limit response.
   */
  @Nested
  @DisplayName("Given API rate limits requests with Retry-After header")
  class GivenRateLimitWithRetryAfter {

    /**
     * Test scenario name for retry handling with Retry-After header.
     */
    private static final String RETRY_SCENARIO = "jobPosting-retry-retry-after";

    @BeforeEach
    void setupRetryAfterScenario() {

      wireMockServer.resetScenarios();

      String url = PATH
          + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
          + JOB_POSTING_ID_PATH;

      wireMockServer.stubFor(
          get(urlPathEqualTo(url))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(url))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("jobPosting.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should respect Retry-After and succeed")
    void shouldRespectRetryAfter() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleJobPostingRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the job posting API returns null data in the response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the job posting API response contains null data.
   */
  @Nested
  @DisplayName("Given job posting API returns null data in the response")
  class GivenNullDataInResponse {

    @BeforeEach
    void setupNullDataInResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH
              + String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, TENANT_ID)
              + JOB_POSTING_ID_PATH))
              .willReturn(aResponse().withStatus(200)
                  .withBody(JSON_DATA_BODY_WITH_NULL_RESPONSE)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponseForNullDataInResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleJobPostingRequest()).next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest singleJobPostingRequest() {
    return LdapSearchRequest.create(
        DN.from("jobPosting=1234,ou=jobPosting,ou=Workday"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest paginatedRequest() {
    return LdapSearchRequest.create(
        DN.from("ou=jobPostings"),
        SearchScope.SUB,
        SearchFilter.from("(objectclass=*)"));
  }
}
