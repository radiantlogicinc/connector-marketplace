package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.WORKERS_ENDPOINT_SUB_PATH;
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
 * Integration tests for worker-related functionality.
 *
 * <p>Uses a per-class test instance lifecycle to allow shared state
 * and non-static setup across test methods.</p>
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
class WorkerIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * Path used to identify the worker resource in API requests.
   */
  private static final String WORKER_ID = "/cc1rgdhdfxsdxgrsf0000";

  @BeforeEach
  void setUp() {
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(
        Collections.singletonList("worker"));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectForWorker()));
    wireMockServer.stubFor(post(
        urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID))).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  /**
   * Tests for scenarios where the workers API returns valid responses.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * worker data is successfully returned by the API.
   */
  @Nested
  @DisplayName("Given workers API returns valid responses")
  class GivenWorkersApiReturnsValidResponses {

    @BeforeEach
    void setupWorkersApi() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("workers.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return all workers")
    void shouldReturnAllWorkers() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              LdapSearchRequest.create(
                  DN.from("ou="),
                  SearchScope.BASE,
                  SearchFilter.from("(objectclass=*)")));

      assertThat(count(dataProvider)).isEqualTo(4);
    }
  }

  /**
   * Tests for scenarios where a worker exists in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * worker data is available.
   */
  @Nested
  @DisplayName("Given a worker exists in the system")
  class GivenWorkerExists {

    @BeforeEach
    void setupWorker() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + WORKER_ID))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("worker.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the worker")
    void shouldReturnWorker() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              getSingleWorkerLdapRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the worker API returns an invalid response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the worker API response is invalid.
   */
  @Nested
  @DisplayName("Given worker API returns invalid response")
  class GivenInvalidWorkerResponse {

    @BeforeEach
    void setupInvalidResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + WORKER_ID))
              .willReturn(aResponse()
                  .withStatus(200)
                  .withBody(TestUtil.getResponseFromFile("invalid_response.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              getSingleWorkerLdapRequest());

      LdapResponse<String> response = (LdapResponse<String>) dataProvider.next();
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
          get(urlPathEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + WORKER_ID))
              .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnError() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              getSingleWorkerLdapRequest());

      assertThat(dataProvider.next().getStatus()).isEqualTo(OPERATIONS_ERROR);
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
    void shouldReturnError() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              getSingleWorkerLdapRequest());

      assertThat(dataProvider.next().getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where the workers API supports pagination.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * paginated worker data retrieval is enabled.
   */
  @Nested
  @DisplayName("Given workers API supports pagination")
  class GivenPaginationIsEnabled {

    @BeforeEach
    void setupPagination() {

      wireMockServer.stubFor(
          get(urlEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + "?limit=100&offset=0"))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("paginated_workers.json"))));


      wireMockServer.stubFor(
          get(urlEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + "?limit=100&offset=100"))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("workers.json"))));

    }

    @Test
    @SneakyThrows
    @DisplayName("Should return all workers across pages")
    void shouldReturnAllWorkersAcrossPages() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              LdapSearchRequest.create(
                  DN.from("ou=workers"),
                  SearchScope.SUB,
                  SearchFilter.from("(objectclass=*)")));

      assertThat(count(dataProvider)).isEqualTo(8);
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
          get(urlPathEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + WORKER_ID))
              .inScenario("retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(PATH + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
              + WORKER_ID))
              .inScenario("retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("worker.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should retry and succeed")
    void shouldRetryAndSucceed() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(
              getSingleWorkerLdapRequest());

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
    private static final String RETRY_SCENARIO = "retry-with-retry-after";

    @BeforeEach
    void setupRetryAfterScenario() {
      wireMockServer.resetScenarios();
      String url = PATH
          + String.format(WORKERS_ENDPOINT_SUB_PATH, TENANT_ID)
          + WORKER_ID;

      wireMockServer.stubFor(
          get(urlPathEqualTo(url)).inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND)
      );

      wireMockServer.stubFor(
          get(urlPathEqualTo(url)).inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("worker.json")))
      );
    }

    @Test
    @SneakyThrows
    @DisplayName("Should respect Retry-After and succeed")
    void shouldRespectRetryAfter() {
      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(getSingleWorkerLdapRequest());
      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest getSingleWorkerLdapRequest() {
    return LdapSearchRequest.create(
        DN.from("workerId=cc1rgdhdfxsdxgrsf0000,ou=workers,ou=Workday"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

}
