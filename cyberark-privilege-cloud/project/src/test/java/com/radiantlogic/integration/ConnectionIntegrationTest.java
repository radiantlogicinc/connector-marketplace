package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.radiantlogic.CyberArkConnector;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.TestConnectionRequest;
import com.radiantlogic.iddm.base.TestConnectionResponse;
import com.radiantlogic.iddm.base.Uri;
import com.radiantlogic.util.TestUtils;
import java.time.Instant;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class ConnectionIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  CyberArkConnector cyberArkConnector;
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
    connectionProperties = TestUtils.getConnectionProperties();
    cyberArkConnector = new CyberArkConnector(targetSchemaObjects, schema, connectionProperties);
    wireMockServer = new WireMockServer(
        WireMockConfiguration.options().notifier(new ConsoleNotifier(true))
            .port(PORT_NUMBER_FOR_TESTING));
    wireMockServer.start();
  }

  @Test
  @SneakyThrows
  void testConnection() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);
    cyberArkConnector.getRequestExecutor().setExpiration(Instant.now().plusSeconds(900));
    cyberArkConnector.getRequestExecutor().setTokenWithBearerPrefixed("Bearer ey..");
    TestConnectionResponse response = cyberArkConnector.testConnection(connectionRequest);
    assertTrue(response.isSuccessful());
  }

  @Test
  @SneakyThrows
  void testConnectionWithInvalidCredentials() {
    Uri uri = Uri.fromString("");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);
    cyberArkConnector.getRequestExecutor().setExpiration(Instant.ofEpochSecond(0));
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(403)));
    TestConnectionResponse response = cyberArkConnector.testConnection(connectionRequest);
    assertFalse(response.isSuccessful());
  }

  @Test
  @SneakyThrows
  void testConnectionWithUriTargetNonEmpty() {
    Uri uri = Uri.fromString("http://localhost");
    TestConnectionRequest connectionRequest = new TestConnectionRequest(uri);
    TestConnectionResponse response = cyberArkConnector.testConnection(connectionRequest);
    assertFalse(response.isSuccessful());
  }
}