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
 * Integration tests for retrieving integration system users from Workday.
 *
 * <p>Verifies that the Workday connector successfully retrieves integration
 * system user data from the configured custom report endpoint and correctly maps the response to
 * connector objects. External Workday service interactions are simulated using WireMock.
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class IntegrationSystemUserIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * Integration System users report endpoint used in tests.
   */
  private static final String INTEGRATION_SYSTEM_USERS_REPORT_URL =
      "/ccx/service/customreport2/test_tenant/test_username"
          + "/All_Integration_System_User_Accounts_Test";

  /**
   * JSON file containing the integration System users response data.
   */
  private static final String INTEGRATION_SYSTEM_USERS_RESPONSE = "integrationSystemUsers.json";

  @BeforeEach
  void setUp() {
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY))
        .thenReturn(Collections.singletonList("integrationsystemuser"));
    when(schema.getSchemaObject(any()))
        .thenReturn(Optional.of(TestUtil.getSchemaObjectForIntegrationSystemUser()));
    wireMockServer.stubFor(post(
        urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
        .willReturn(aResponse()
            .withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  /**
   * Tests for scenarios where integration system users exist.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * integration system users are available.
   */
  @Nested
  @DisplayName("Given integration system users exist")
  class GivenIntegrationSystemUsersExist {

    @BeforeEach
    void setup() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(INTEGRATION_SYSTEM_USERS_RESPONSE))));
    }

    @Test
    @DisplayName("Should return all integration system users")
    void shouldReturnAllIntegrationSystemUsers() throws Exception {
      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(baseRequest());
      assertThat(count(dataProvider)).isEqualTo(7);
    }
  }

  /**
   * Tests for scenarios where an integration system user exists in the system.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * an integration system user is available.
   */
  @Nested
  @DisplayName("Given a integration system user exists in the system")
  class GivenIntegrationSystemUserExists {

    @BeforeEach
    void setupIntegrationSystemUser() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(INTEGRATION_SYSTEM_USERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return the integration system user")
    void shouldReturnIntegrationSystemUser() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleIntegrationSystemUserRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the integration system user API returns an invalid response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API response for an integration system user is invalid.
   */
  @Nested
  @DisplayName("Given integration system user API returns invalid response")
  class GivenInvalidIntegrationSystemUserResponse {

    @BeforeEach
    void setupInvalidResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile("invalid_response.json"))));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleIntegrationSystemUserRequest())
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
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleIntegrationSystemUserRequest())
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
          (LdapResponse<String>) workdayConnector.search(singleIntegrationSystemUserRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
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
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario("integrationSystemUser-retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario("integrationSystemUser-retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(INTEGRATION_SYSTEM_USERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should retry and succeed")
    void shouldRetryAndSucceed() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleIntegrationSystemUserRequest());

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
    private static final String RETRY_SCENARIO = "integrationSystemUser-retry-retry-after";

    @BeforeEach
    void setupRetryAfterScenario() {

      wireMockServer.resetScenarios();

      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .inScenario(RETRY_SCENARIO)
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(INTEGRATION_SYSTEM_USERS_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should respect Retry-After and succeed")
    void shouldRespectRetryAfter() {

      SequentialDataProvider<LdapResponse<?>> dataProvider =
          workdayConnector.search(singleIntegrationSystemUserRequest());

      assertThat(count(dataProvider)).isEqualTo(1);
    }
  }

  /**
   * Tests for scenarios where the integration system user API returns null data in the response.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the integration system user API response contains null data.
   */
  @Nested
  @DisplayName("Given integration system user API returns null data in the response")
  class GivenNullDataInResponse {

    @BeforeEach
    void setupNullDataInResponse() {
      wireMockServer.stubFor(
          get(urlPathEqualTo(INTEGRATION_SYSTEM_USERS_REPORT_URL))
              .withQueryParam(FORMAT, equalTo(FORMAT_TYPE))
              .willReturn(aResponse().withStatus(200)
                  .withBody(JSON_BODY_WITH_NULL_RESPONSE)));
    }

    @Test
    @SneakyThrows
    @DisplayName(ERROR_RESPONSE)
    void shouldReturnErrorResponseForNullDataInResponse() {

      LdapResponse<String> response =
          (LdapResponse<String>) workdayConnector.search(singleIntegrationSystemUserRequest())
              .next();

      assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
    }
  }

  @SuppressWarnings("PMD.AccessorMethodGeneration")
  private LdapSearchRequest singleIntegrationSystemUserRequest() {
    return LdapSearchRequest.create(
        DN.from("username=wd-integration,ou=integrationSystemUser,ou=Workday"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

}
