package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.WorkdayConnector;
import com.radiantlogic.iddm.base.TestConnectionRequest;
import com.radiantlogic.iddm.base.TestConnectionResponse;
import com.radiantlogic.iddm.base.Uri;
import com.radiantlogic.utility.TestUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration test suite for validating Workday connector connectivity behavior.
 *
 * <p>This test class extends {@link AbstractWorkdayIntegrationTest} and verifies the connector's
 * ability to establish and maintain connectivity under various real-world scenarios including
 * successful authentication, error responses, and retry conditions. The tests are executed using
 * infrastructure such as WireMock server setup and connector initialization to be reused
 * efficiently across all test methods.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts"})
@DisplayName("Workday Connector - Connection Integration Tests")
class ConnectionIntegrationTest extends AbstractWorkdayIntegrationTest {

  /**
   * JSON file containing the access token response data.
   */
  private static final String ACCESS_TOKEN_RESPONSE = "access_token.json";


  /**
   * Tests for scenarios where the authentication service returns a valid token.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * authentication succeeds.
   */
  @Nested
  @DisplayName("Given the authentication service returns a valid token")
  class GivenSuccessfulAuthentication {

    @BeforeEach
    void stubSuccess() {
      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse()
              .withBody(TestUtil.getResponseFromFile(ACCESS_TOKEN_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should successfully establish connection")
    void shouldSuccessfullyConnect() {
      Uri uri = Uri.fromString("");
      TestConnectionRequest request = new TestConnectionRequest(uri);

      TestConnectionResponse response = workdayConnector.testConnection(request);

      assertThat(response.isSuccessful()).isTrue();
    }
  }

  /**
   * Tests for scenarios where the token response contains invalid JSON.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the authentication token response cannot be parsed.
   */
  @Nested
  @DisplayName("Given the token response contains invalid JSON")
  class GivenInvalidJsonResponse {

    @BeforeEach
    void stubInvalidJson() {
      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse()
              .withStatus(200)
              .withBody("bad-json")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fail connection")
    void shouldFailConnection() {
      TestConnectionRequest request =
          new TestConnectionRequest(Uri.fromString(""));

      TestConnectionResponse response =
          workdayConnector.testConnection(request);

      assertThat(response.isSuccessful()).isFalse();
    }
  }

  /**
   * Tests for scenarios where the service is unavailable.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * an I/O exception occurs during service communication.
   */
  @Nested
  @DisplayName("Given the service is unavailable")
  class GivenIoException {

    @BeforeEach
    void stubIoException() {
      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fail connection when IO error occurs")
    void shouldFailConnection() {
      TestConnectionRequest request =
          new TestConnectionRequest(Uri.fromString(""));

      TestConnectionResponse response =
          workdayConnector.testConnection(request);

      assertThat(response.isSuccessful()).isFalse();
    }
  }

  /**
   * Tests for scenarios where the API returns a rate limit response before succeeding.
   *
   * <p>This nested test class groups test cases that verify retry behavior when
   * the API initially returns a 429 response and later succeeds.
   */
  @Nested
  @DisplayName("Given the API returns 429 before succeeding")
  class GivenRateLimitedThenSuccess {

    @BeforeEach
    void stubRetryScenario() {
      wireMockServer.stubFor(
          post(urlEqualTo(PATH
              + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .inScenario("retry")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse().withStatus(429))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          post(urlEqualTo(PATH
              + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .inScenario("retry")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(ACCESS_TOKEN_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should succeed after retrying")
    void shouldSucceedAfterRetry() {
      TestConnectionResponse response =
          workdayConnector.testConnection(
              new TestConnectionRequest(Uri.fromString(""))
          );

      assertThat(response.isSuccessful()).isTrue();
    }
  }

  /**
   * Tests for scenarios where the API returns a Retry-After header before succeeding.
   *
   * <p>This nested test class groups test cases that verify retry behavior when
   * the API provides retry timing information before a successful response.
   */
  @Nested
  @DisplayName("Given API returns Retry-After header before succeeding")
  class GivenRetryAfterHeader {

    @BeforeEach
    void stubRetryAfter() {
      wireMockServer.stubFor(
          post(urlEqualTo(PATH
              + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .inScenario("retry-after")
              .whenScenarioStateIs(Scenario.STARTED)
              .willReturn(aResponse()
                  .withStatus(429)
                  .withHeader("Retry-After", "1"))
              .willSetStateTo(SECOND));

      wireMockServer.stubFor(
          post(urlEqualTo(PATH
              + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .inScenario("retry-after")
              .whenScenarioStateIs(SECOND)
              .willReturn(aResponse()
                  .withBody(TestUtil.getResponseFromFile(ACCESS_TOKEN_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should succeed after honoring Retry-After header")
    void shouldSucceedAfterRetryAfter() {
      TestConnectionResponse response =
          workdayConnector.testConnection(
              new TestConnectionRequest(Uri.fromString(""))
          );

      assertThat(response.isSuccessful()).isTrue();
    }
  }

  /**
   * Tests for scenarios where invalid credentials are provided.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * authentication fails due to invalid credentials.
   */
  @Nested
  @DisplayName("Given invalid credentials")
  class GivenInvalidCredentials {

    @BeforeEach
    void stubForbidden() {
      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse().withStatus(403)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fail to connect")
    void shouldFailConnection() {
      TestConnectionResponse response =
          workdayConnector.testConnection(
              new TestConnectionRequest(Uri.fromString(""))
          );

      assertThat(response.isSuccessful()).isFalse();
    }
  }

  /**
   * Tests for scenarios where the request is malformed.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the API receives an invalid request.
   */
  @Nested
  @DisplayName("Given the request is malformed")
  class GivenBadRequest {

    @BeforeEach
    void stubBadRequest() {
      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse().withStatus(400)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fail to connect")
    void shouldFailConnection() {
      TestConnectionResponse response =
          workdayConnector.testConnection(
              new TestConnectionRequest(Uri.fromString(""))
          );

      assertThat(response.isSuccessful()).isFalse();
    }
  }

  /**
   * Tests for scenarios where the instance is in hibernation state.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the service instance is unavailable due to hibernation.
   */
  @Nested
  @DisplayName("Given the instance is in hibernation state")
  class GivenHibernateState {

    @BeforeEach
    void stubInvalidResponse() {
      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse()
              .withBody(TestUtil.getResponseFromFile("access_token.html"))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fail to connect")
    void shouldFailConnection() {
      TestConnectionResponse response =
          workdayConnector.testConnection(
              new TestConnectionRequest(Uri.fromString(""))
          );

      assertThat(response.isSuccessful()).isFalse();
    }
  }

  /**
   * Tests for scenarios where the response contains a blank access token.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the authentication response does not provide a valid access token.
   */
  @Nested
  @DisplayName("Given blank access token in response")
  class GivenBlankAccessTokenInResponse {

    @BeforeEach
    void stubBlankTokenResponse() {

      wireMockServer.stubFor(
          post(urlEqualTo(PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
              .willReturn(aResponse()
                  .withStatus(200)
                  .withBody("{\"access_token\": \"\"}")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should fail to connect when access token is blank")
    void shouldFailToConnect() {

      TestConnectionRequest request =
          new TestConnectionRequest(Uri.fromString(""));
      TestConnectionResponse response =
          workdayConnector.testConnection(request);
      assertThat(response.isSuccessful()).isFalse();
    }
  }

  /**
   * Tests for scenarios where a cached token is already present.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * an existing cached token is available.
   */
  @Nested
  @DisplayName("Given cached token already present")
  class GivenCachedTokenAlreadyPresent {

    @BeforeEach
    void stubCachedTokenAlreadyPresent() {

      wireMockServer.stubFor(post(urlEqualTo(PATH
          + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
          .willReturn(aResponse()
              .withBody(TestUtil.getResponseFromFile(ACCESS_TOKEN_RESPONSE))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should always re-fetch the token regardless of cached value")
    void shouldAlwaysReFetchTokenRegardlessOfCachedValue() {
      TestConnectionRequest request =
          new TestConnectionRequest(Uri.fromString(""));

      TestConnectionResponse testConnectionResponse1 =
          workdayConnector.testConnection(request);

      TestConnectionResponse testConnectionResponse2 =
          workdayConnector.testConnection(request);

      assertThat(testConnectionResponse1).isNotNull();
      assertThat(testConnectionResponse2).isNotNull();

      assertThat(testConnectionResponse1.isSuccessful()).isTrue();
      assertThat(testConnectionResponse2.isSuccessful()).isTrue();

      wireMockServer.verify(
          2,
          postRequestedFor(urlEqualTo(
              PATH + String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, TENANT_ID)))
      );
    }
  }

  /**
   * Tests for scenarios where an invalid private key is configured.
   *
   * <p>This nested test class groups test cases that verify behavior when
   * the configured private key is invalid.
   */
  @Nested
  @DisplayName("Given an invalid private key is configured")
  class GivenInvalidPrivateKey {

    @Test
    @SneakyThrows
    @DisplayName("Should fail to connect when loaded invalid private key")
    void shouldFailToConnectWhenInvalidPrivateKeyIsConfigured() {
      // Configured connector with invalid private key configuration
      WorkdayConnector workdayConnector = new WorkdayConnector(
          targetSchemaObjects,
          schema,
          TestUtil.getInvalidConnectionProperties(),
          httpClient);

      TestConnectionRequest request =
          new TestConnectionRequest(Uri.fromString(""));

      TestConnectionResponse response =
          workdayConnector.testConnection(request);

      assertThat(response.isSuccessful()).isFalse();
    }
  }

}