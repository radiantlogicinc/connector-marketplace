package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.ServiceNowConnector;
import com.radiantlogic.http.HttpClient;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.TestConnectionRequest;
import com.radiantlogic.iddm.base.TestConnectionResponse;
import com.radiantlogic.iddm.base.Uri;
import com.radiantlogic.util.TestUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  ServiceNowConnector serviceNowConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;

  @AfterEach
  void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void setUp() {
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    connectionProperties = TestUtil.getConnectionProperties();
    HttpClient httpClient = new HttpClient(connectionProperties);
    serviceNowConnector =
        new ServiceNowConnector(targetSchemaObjects, schema, connectionProperties, httpClient);
    wireMockServer = new WireMockServer(
        WireMockConfiguration.options().notifier(new ConsoleNotifier(true))
            .port((int) connectionProperties.get(URL_PORT_KEY)));
    wireMockServer.start();
  }

  @Test
  @SneakyThrows
  void testConnectionWithSuccess() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));
    TestConnectionResponse response = serviceNowConnector.testConnection(connectionRequest);
    assertTrue(response.isSuccessful());
  }

  @Test
  @SneakyThrows
  void testConnectionWithInstanceInHibernateState() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.html"))));
    TestConnectionResponse response = serviceNowConnector.testConnection(connectionRequest);
    assertFalse(response.isSuccessful());
  }

  @Test
  @SneakyThrows
  void testConnectionWithTooManyRequestsAndSuccess() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario(
                "testConnectionWithTooManyRequestsAndSuccess")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario(
                "testConnectionWithTooManyRequestsAndSuccess")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));

    TestConnectionResponse response = serviceNowConnector.testConnection(connectionRequest);
    assertTrue(response.isSuccessful());
  }

  @Test
  @SneakyThrows
  void testConnectionWithTooManyRequestsAndSuccessWithRetryAfterHeader() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT))
            .inScenario("testConnectionWithTooManyRequestsAndSuccessWithRetryAfterHeader")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Retry-After", "1"))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario(
                "testConnectionWithTooManyRequestsAndSuccessWithRetryAfterHeader")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));

    TestConnectionResponse response = serviceNowConnector.testConnection(connectionRequest);
    assertTrue(response.isSuccessful());
  }

  @Test
  @SneakyThrows
  void testConnectionWithInvalidCredentials() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(403)));
    TestConnectionResponse response = serviceNowConnector.testConnection(connectionRequest);
    assertFalse(response.isSuccessful());
  }
}
