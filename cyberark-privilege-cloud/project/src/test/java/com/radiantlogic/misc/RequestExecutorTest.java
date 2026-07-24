package com.radiantlogic.misc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.radiantlogic.utility.Constants.CLIENT_CREDENTIALS_GRANT_TYPE;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_SECRET_KEY;
import static com.radiantlogic.utility.Constants.CONTENT_TYPE_KEY;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.GRANT_TYPE_KEY;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static com.radiantlogic.utility.Constants.URL_ENCODED_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.util.TestUtils;
import java.time.Instant;
import lombok.SneakyThrows;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class RequestExecutorTest {

  RequestBuilder requestBuilder;
  RequestExecutor requestExecutor;
  private WireMockServer wireMockServer;
  ReadOnlyProperties connectionProperties;

  @AfterEach
  void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    connectionProperties = TestUtils.getConnectionProperties();
    requestExecutor = new RequestExecutor(connectionProperties, requestBuilder);
    wireMockServer = new WireMockServer(
        WireMockConfiguration.options().notifier(new ConsoleNotifier(true))
            .port(PORT_NUMBER_FOR_TESTING));
    wireMockServer.start();
  }

  @Test
  @SneakyThrows
  void checkExistingTokenNotExpired() {
    requestExecutor.setExpiration(Instant.now().plusSeconds(900));
    requestExecutor.setTokenWithBearerPrefixed("Bearer ey..");
    assertTrue(requestExecutor.canConnect());
  }

  @Test
  @SneakyThrows
  void checkExistingTokenExpiredAndNewGenerated() {
    requestExecutor.setExpiration(Instant.now().minusSeconds(900));
    Request request = getTokenRequest();
    when(requestBuilder.getTokenRequest()).thenReturn(request);
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));
    assertTrue(requestExecutor.canConnect());
  }

  @Test
  @SneakyThrows
  void checkExistingTokenExpiredAndNewGeneratedWithRetry() {
    requestExecutor.setExpiration(Instant.now().minusSeconds(900));
    Request request = getTokenRequest();
    when(requestBuilder.getTokenRequest()).thenReturn(request);

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario("generateTokenWithRetry")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(status(429))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario("generateTokenWithRetry")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(status(429))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario("generateTokenWithRetry")
            .whenScenarioStateIs("THIRD_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));

    assertTrue(requestExecutor.canConnect());
  }


  @Test
  @SneakyThrows
  void checkExistingTokenExpiredAndNewGeneratedWithRetryAfterExceptions() {
    requestExecutor.setExpiration(Instant.now().minusSeconds(900));
    Request request = getTokenRequest();
    when(requestBuilder.getTokenRequest()).thenReturn(request);

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario("generateTokenWithRetry")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(aResponse().withFixedDelay(2000))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario("generateTokenWithRetry")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(status(429))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).inScenario("generateTokenWithRetry")
            .whenScenarioStateIs("THIRD_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));

    assertTrue(requestExecutor.canConnect());
  }

  @Test
  @SneakyThrows
  void checkExistingTokenExpiredAndNewNotGenerated() {
    requestExecutor.setExpiration(Instant.now().minusSeconds(900));
    Request request = getTokenRequest();
    when(requestBuilder.getTokenRequest()).thenReturn(request);
    wireMockServer.stubFor(
        post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(status(403)));
    assertFalse(requestExecutor.canConnect());
  }


  Request getTokenRequest() {
    return new Request.Builder().url(getAccessTokenUrl()).post(getFormBodyForAccessToken())
        .addHeader(CONTENT_TYPE_KEY, URL_ENCODED_CONTENT_TYPE).build();
  }

  HttpUrl getAccessTokenUrl() {
    return TestUtils.getBaseUrl().addPathSegments(GENERATE_ACCESS_TOKEN_ENDPOINT).build();
  }


  FormBody getFormBodyForAccessToken() {
    return new FormBody.Builder().addEncoded(GRANT_TYPE_KEY, CLIENT_CREDENTIALS_GRANT_TYPE)
        .addEncoded(CLIENT_ID_KEY, "client id").addEncoded(CLIENT_SECRET_KEY, "client secret")
        .build();
  }
}
