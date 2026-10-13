package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.utility.TestUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for business process functionality.
 *
 * <p>This test class verifies the behavior of business process operations
 * exposed by the Workday integration layer. The tests validate that requests are processed
 * correctly, expected responses are returned, and the integration behaves as expected when
 * interacting with external services.
 *
 * <p>This class extends {@link AbstractWorkdayIntegrationTest} to reuse the
 * common integration test configuration, test infrastructure, and shared utilities required for
 * executing Workday integration tests.
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts"})
public class BusinessProcessIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * Business processes report endpoint used in tests.
   */
  private static final String BUSINESS_PROCESSES_REPORT_URL =
      "/ccx/service/customreport2/test_tenant/test_username"
          + "/BusinessProcesses_Test";

  /**
   * JSON file containing the business processes response data.
   */
  private static final String BUSINESS_PROCESSES_RESPONSE = "businessProcesses.json";

  @BeforeEach
  void setUp() {
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY))
        .thenReturn(Collections.singletonList("businessProcess"));
    when(schema.getSchemaObject(any()))
        .thenReturn(Optional.of(TestUtil.getSchemaObjectForBusinessProcess()));
    wireMockServer.stubFor(post(
        urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
        .willReturn(aResponse()
            .withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  /**
   * Tests for scenarios where business processes exist.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * business processes are available.
   */
  @Nested
  @DisplayName("Given business processes exist")
  class GivenBusinessProcessesExist {

    @BeforeEach
    void setup() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(BUSINESS_PROCESSES_RESPONSE))));
    }

    @Test
    @DisplayName("Should return all business processes")
    void shouldReturnAllBusinessProcesses() throws Exception {
      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(baseRequest());
      assertThat(count(dataProvider)).isEqualTo(20);
    }
  }

  /**
   * Tests for scenarios where a business process exists in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * a business process is available.
   */
  @Nested
  @DisplayName("Given a business process exists in the system")
  class GivenBusinessProcessExists {

    @BeforeEach
    void setupBusinessProcess() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(BUSINESS_PROCESSES_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the business process")
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void shouldReturnBusinessProcess() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleBusinessProcessRequest());

      ObjectMapper objectMapper = new ObjectMapper();

      int businessProcessesFetched = 0;

      while (dataProvider.hasMore()) {
        String response = (String) dataProvider.next().getData();
        List<Map<String, Object>> list =
            objectMapper.readValue(response,
                new TypeReference<List<Map<String, Object>>>() {
                });
        Map<String, Object> entry = list.get(0);
        businessProcessesFetched += list.size();
        assertThat(entry.get("WorkdayId")).isEqualTo("11d8e9f0a1b2c3456677889900112233");
        assertThat(entry.get("Name")).isEqualTo("Quality Inspection Report");
        assertThat(entry.get("Description")).isEqualTo(
            "Generates quality inspection reports for manufactured products.");
        assertThat(entry.get("FunctionalAreas")).isEqualTo("Quality Assurance");
      }
      assertThat(businessProcessesFetched).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the business process API returns an invalid response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API response is invalid.
   */
  @Nested
  @DisplayName("Given business process API returns invalid response")
  class GivenInvalidBusinessProcessResponse {

    @BeforeEach
    void setupInvalidResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("invalid_response.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleBusinessProcessRequest())
              .next();

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
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleBusinessProcessRequest())
              .next();

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
          (LdapResponse<String>) workdayConnector.search(singleBusinessProcessRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  /**
   * Tests for scenarios where the API rate limits requests without a header.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * rate limit information is unavailable in the response header.
   */
  @Nested
  @DisplayName("Given API rate limits requests")
  class GivenRateLimitWithoutHeader {

    @BeforeEach
    void setupRetryScenario() {

      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario("businessProcess-retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario("businessProcess-retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(BUSINESS_PROCESSES_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should retry and succeed")
    void shouldRetryAndSucceed() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleBusinessProcessRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the API rate limits requests with a Retry-After header.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * rate limit information is provided in the response header.
   */
  @Nested
  @DisplayName("Given API rate limits requests with Retry-After header")
  class GivenRateLimitWithRetryAfter {

    /**
     * Test scenario name for retry handling with Retry-After header.
     */
    private static final String RETRY_SCENARIO = "businessProcess-retry-retry-after";

    @BeforeEach
    void setupRetryAfterScenario() {

      wireMockServer.resetScenarios();

      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(BUSINESS_PROCESSES_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should respect Retry-After and succeed")
    void shouldRespectRetryAfter() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleBusinessProcessRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the business process API returns null data in the response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API response contains null data.
   */
  @Nested
  @DisplayName("Given business process API returns null data in the response")
  class GivenNullDataInResponse {

    @BeforeEach
    void setupNullDataInResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(BUSINESS_PROCESSES_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse().withStatus(200)
                  .withBody(JSON_BODY_WITH_NULL_RESPONSE)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponseForNullDataInResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleBusinessProcessRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest singleBusinessProcessRequest() {
    return LdapSearchRequest.create(
        DN.from("name=Quality Inspection Report,ou=businessProcess,ou=Workday"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

}

