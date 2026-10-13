package com.radiantlogic.http;

import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.util.TestUtil;
import java.time.Instant;
import lombok.SneakyThrows;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestBuilderTest {

  RequestBuilder requestBuilder;
  RequestExecutor requestExecutor;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;

  @BeforeEach
  void setUp() {
    connectionProperties = TestUtil.getConnectionProperties();
    requestExecutor = mock(RequestExecutor.class);
    requestBuilder = new RequestBuilder(connectionProperties, requestExecutor);
    wireMockServer = new WireMockServer(
        WireMockConfiguration.options().notifier(new ConsoleNotifier(true))
            .port((int) connectionProperties.get(URL_PORT_KEY)));
    wireMockServer.start();
  }

  @AfterEach
  void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @Test
  @SneakyThrows
  void getTokenRequest() {
    assertNotNull(requestBuilder.getTokenRequest(), "");
  }

  private RequestBuilder.BearerToken getBearerToken() {
    return new RequestBuilder.BearerToken("Bearer ey...", Instant.now().plusSeconds(900));
  }

  @Test
  @SneakyThrows
  void checkExistingTokenNotExpired() {
    RequestBuilder.BearerToken bearerToken = getBearerToken();

    requestBuilder.setBearerToken(bearerToken);
    assertTrue(requestBuilder.canConnectWithValidToken());
  }

  @Test
  @SneakyThrows
  void checkExistingTokenWithExpired() {
    RequestBuilder.BearerToken bearerToken =
        new RequestBuilder.BearerToken("Bearer ey...", Instant.now().minusSeconds(500));
    requestBuilder.setBearerToken(bearerToken);
    ResponseData responseData =
        new ResponseData(TestUtil.getResponseFromFile("access_token.json"), "", 200,
            new Headers.Builder().build());
    when(requestExecutor.execute(any())).thenReturn(responseData);
    assertTrue(requestBuilder.canConnectWithValidToken());
  }

  @Test
  @SneakyThrows
  void getUsersRequestWithoutNextLink() {
    RequestBuilder.BearerToken bearerToken = getBearerToken();
    requestBuilder.setBearerToken(bearerToken);
    assertNotNull(requestBuilder.getUsersRequest("", ""));
  }

  @Test
  @SneakyThrows
  void getUsersRequestWithNextLink() {
    RequestBuilder.BearerToken bearerToken = getBearerToken();
    requestBuilder.setBearerToken(bearerToken);
    assertNotNull(requestBuilder.getUsersRequest(
        "http://localhost:50000/api/now/table/sys_user?sysparm_limit=1&sysparm_offset=1", ""));
  }

  @Test
  @SneakyThrows
  void getUsersRequestWithSysId() {
    RequestBuilder.BearerToken bearerToken = getBearerToken();
    requestBuilder.setBearerToken(bearerToken);
    assertNotNull(requestBuilder.getUsersRequest(
        "", "test_user_name"));
  }
}
