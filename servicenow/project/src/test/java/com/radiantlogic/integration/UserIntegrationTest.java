package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.iddm.ldap.LdapResultCode.SUCCESS;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.USERS_ENDPOINT_SUB_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.gson.reflect.TypeToken;
import com.radiantlogic.ServiceNowConnector;
import com.radiantlogic.http.HttpClient;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.util.TestUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserIntegrationTest {

  private static final String USERS_DN = "user=test_user_name,ou=Users,ou=ServiceNow";

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
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtil.getSchemaObjectForUser()));
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));
  }

  @Test
  @SneakyThrows
  void getUser() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    LdapSearchRequest singleUserLdapRequest = getSingleUserLdapRequest();
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);
    int usersFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      usersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, usersFetched);
  }

  @Test
  @SneakyThrows
  void getUserContainingCaretSymbolWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user_with_caret.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest singleUserLdapRequest = LdapSearchRequest.create(
        DN.from("user=user^1,ou=Users,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);

    int usersFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      usersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, usersFetched);
  }

  @Test
  @SneakyThrows
  void getUserWithInvalidResponse() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("invalid_get_user_response.json"))));
    LdapSearchRequest singleUserLdapRequest = getSingleUserLdapRequest();
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getPaginatedUsers() {
    LdapSearchRequest ldapSearchRequestToGetAllUsers =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(objectclass=*)"));

    wireMockServer.stubFor(get(urlEqualTo("/api/now/table/sys_user?sysparm_limit=1")).willReturn(
        aResponse().withHeader("Link",
                "<http://localhost:50000/api/now/table/sys_user?sysparm_limit=1&sysparm_offset=0"
                    + ">;rel=\"first\",<http://localhost:50000/api/now/table/sys_user?sysparm_limit"
                    + "=1&sysparm_offset=1>;rel=\"next\"")
            .withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_user.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            ldapSearchRequestToGetAllUsers);
    int usersFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      usersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, usersFetched);
  }

  @Test
  @SneakyThrows
  void getUserWithBadRequest() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withStatus(400)));
    LdapSearchRequest singleUserLdapRequest = getSingleUserLdapRequest();

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getUserWithInvalidCredentials() {
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(401)));
    LdapSearchRequest singleUserLdapRequest = getSingleUserLdapRequest();
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getUserWithRetry() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user")).inScenario("getUserWithRetryIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(429))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user")).inScenario("getUserWithRetryIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest singleUserLdapRequest = getSingleUserLdapRequest();
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);
    int usersFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      usersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, usersFetched);
  }

  @Test
  @SneakyThrows
  void getUserWithRetryUsingRetryAfterHeader() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user")).inScenario("getUserWithRetryIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(429).withHeader("Retry-After", "1"))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user")).inScenario("getUserWithRetryIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest singleUserLdapRequest = getSingleUserLdapRequest();
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleUserLdapRequest);
    int usersFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      usersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, usersFetched);
  }

  private LdapSearchRequest getSingleUserLdapRequest() {
    return LdapSearchRequest.create(
        DN.from("user=test_user_name,ou=Users,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

  @Test
  @SneakyThrows
  void deleteUserWithSuccess() {
    mockGetUser();
    wireMockServer.stubFor(delete(
        urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH + "/606de4f2c34b321019b4fa0ed40131f2")).willReturn(
        aResponse().withStatus(204)));

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(DN.from(USERS_DN));

    LdapResponse<String> response = serviceNowConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidUserName() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("empty_user_response.json"))));

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(DN.from(USERS_DN));

    LdapResponse<String> response = serviceNowConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidId() {
    mockGetUser();
    wireMockServer.stubFor(delete(
        urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH + "/606de4f2c34b321019b4fa0ed40131f2")).willReturn(
        aResponse().withStatus(404)
            .withBody(TestUtil.getResponseFromFile("delete_invalid_user.json"))));

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(DN.from(USERS_DN));

    LdapResponse<String> response = serviceNowConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserLinkedWithConnectorConfiguredClientId() {
    mockGetUser();
    wireMockServer.stubFor(delete(
        urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH + "/606de4f2c34b321019b4fa0ed40131f2")).willReturn(
        aResponse().withStatus(403)
            .withBody(TestUtil.getResponseFromFile("delete_client_id_linked_user.json"))));

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(DN.from(USERS_DN));

    LdapResponse<String> response = serviceNowConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidCredentials() {
    mockGetUser();
    wireMockServer.stubFor(delete(
        urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH + "/606de4f2c34b321019b4fa0ed40131f2")).willReturn(
        aResponse().withStatus(401)
            .withBody(TestUtil.getResponseFromFile("invalid_credentials.json"))));

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(DN.from(USERS_DN));

    LdapResponse<String> response = serviceNowConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  private LdapModifyRequest getModifyRequest(Modification modification, String dn) {
    return LdapModifyRequest.builder()
        .dn(DN.from(dn))
        .modifications(Lists.newArrayList(modification)).build();
  }

  private void mockCommonStubsForUpdateUser(String fileName, int statusCode) {

    ResponseDefinitionBuilder responseBuilder = aResponse().withStatus(statusCode);
    String sysId = "";
    if (StringUtils.isNotBlank(fileName)) {
      String apiResponse = TestUtil.getResponseFromFile(fileName);
      ServiceNowResponse<UserRow> response =
          SERVICE_NOW_RESPONSE_GSON.fromJson(
              apiResponse,
              new TypeToken<ServiceNowResponse<UserRow>>() {
              }.getType());
      sysId = response.getResult().get(0).getSysId();
      responseBuilder.withBody(apiResponse);
    }
    String url = "/" + USERS_ENDPOINT_SUB_PATH + "/" + sysId;
    wireMockServer.stubFor(patch(urlEqualTo(url)).willReturn(responseBuilder));
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
  }

  @Test
  @SneakyThrows
  void testUpdateUserZipCodeWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("zip", "92600"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserZipCodeWithFailure() {
    mockCommonStubsForUpdateUser("update_user_without_zip.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("zip", "92600"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testDeleteUserZipCodeWithSuccess() {
    mockCommonStubsForUpdateUser("update_user_without_zip.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("zip", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserZipCodeWithMultipleValues() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("zip", Lists.newArrayList("110", "220")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserZipCodeWithInvalidUserName() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("empty_user_response.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("zip", "92600"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserZipCodeWithInvalidUserName() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("empty_user_response.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("zip", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserActiveStatusWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("active", "true"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserActiveStatusWithFailure() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("active", "false"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserActiveStatusWithInvalidValue() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("active", "f."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserActiveStatus() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("active", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserLockedOutStatusWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("LockedOut", "false"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserLockedOutStatusWithFailure() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("LockedOut", "true"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserLockedOutStatusWithInvalidValue() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("LockedOut", "f."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserLockedOutStatus() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("LockedOut", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserInternalIntegrationUserStatusWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("InternalIntegrationUser", "true"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserInternalIntegrationUserStatusWithFailure() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("InternalIntegrationUser", "false"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserInternalIntegrationUserStatusWithInvalidValue() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("InternalIntegrationUser", "f."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserInternalIntegrationUserStatus() {
    mockGetUser();
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("InternalIntegrationUser", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserCountryWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("Country", "es"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserCountryWithWrongCountryCode() {
    mockCommonStubsForUpdateUser("update_user_without_country.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("Country", "es."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testDeleteUserCountryWithSuccess() {
    mockCommonStubsForUpdateUser("update_user_without_country.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("Country", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserGenderWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("gender", "not specified"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserGenderWithIncorrectGenderCode() {
    mockCommonStubsForUpdateUser("update_user_without_gender.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("gender", "male."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testDeleteUserGenderWithSuccess() {
    mockCommonStubsForUpdateUser("update_user_without_gender.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("gender", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIntroductionWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("Introduction", "mr."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIntroductionWithIncorrectIntroductionCode() {
    mockCommonStubsForUpdateUser("update_user_without_introduction.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("Introduction", "mm"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testDeleteUserIntroductionWithSuccess() {
    mockCommonStubsForUpdateUser("update_user_without_introduction.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("Introduction", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIdentityTypeWithSuccess() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("IdentityType", "machine"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIdentityTypeWithIncorrectIdentityTypeCode() {
    mockCommonStubsForUpdateUser("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("IdentityType", "hum"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIdentityTypeToBlank() {
    mockCommonStubsForUpdateUser("update_user_with_default_identity_type.json", 200);

    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("another_user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("IdentityType", ""));

    LdapModifyRequest ldapModifyRequest =
        getModifyRequest(modification, "user=dana,ou=Users,ou=ServiceNow");

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testDeleteUserIdentityType() {
    mockCommonStubsForUpdateUser("update_user_with_default_identity_type.json", 200);

    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("another_user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("IdentityType", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest =
        getModifyRequest(modification, "user=dana,ou=Users,ou=ServiceNow");

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserName() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserName", "test_user_name"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserName() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserName", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUserNameOnExistingUser() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserName", "new_username"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserFullName() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("Name", "full name"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserFullName() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("name", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserSysId() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("sysid", "test_sys_id"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserSysId() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("sysid", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserFederatedId() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("FederatedId", "test_federated_id"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserFederatedId() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("FederatedId", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserActiveStatusWithBadRequest() {
    mockGetUser();

    wireMockServer.stubFor(patch(urlPathEqualTo(
        "/" + USERS_ENDPOINT_SUB_PATH + "/606de4f2c34b321019b4fa0ed40131f2")).willReturn(
        aResponse().withStatus(400)));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("active", "true"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  private void mockGetUser() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
  }

  private void stubUserRoleApiWithSuccess(String scenarioName, int numberOfRoles) {
    for (int i = 0; i < numberOfRoles; i++) {
      String currentState = (i == 0) ? Scenario.STARTED : "CALL_" + i;
      String nextState = (i == numberOfRoles - 1) ? null : "CALL_" + (i + 1);
      String sysId = String.format("membership_%s_%03d", scenarioName, i);

      if (nextState != null) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(currentState)
            .willReturn(aResponse().withStatus(201)
                .withBody("{\"result\":{\"sys_id\":\"" + sysId + "\"}}"))
            .willSetStateTo(nextState));
      } else {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(currentState)
            .willReturn(aResponse().withStatus(201)
                .withBody("{\"result\":{\"sys_id\":\"" + sysId + "\"}}")));
      }
    }
  }

  private void verifyUserRoleApiCalls(int expectedCount, String... roleNames) {
    wireMockServer.verify(expectedCount,
        WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));

    for (String roleName : roleNames) {
      wireMockServer.verify(1,
          WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role"))
              .withRequestBody(WireMock.containing("\"role\":\"" + roleName + "\"")));
    }
  }

  @Test
  @SneakyThrows
  void testCreateUserWithSuccess() {
    wireMockServer.stubFor(post(urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH)).willReturn(
        aResponse().withStatus(201)
            .withBody(TestUtil.getResponseFromFile("create_user_success.json"))));

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(TestUtil.getBaseUserCreationAttributes())
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithBadRequest() {
    wireMockServer.stubFor(post(urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH)).willReturn(
        aResponse().withStatus(400)));

    List<Attribute> attributes = TestUtil.getMinimalUserAttributes();
    attributes.add(Attribute.create("UserName", "test_user_name"));

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithoutUserName() {
    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(TestUtil.getMinimalUserAttributes())
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidIdentityTypeAndSuccess() {
    wireMockServer.stubFor(post(urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH)).willReturn(
        aResponse().withStatus(201)
            .withBody(TestUtil.getResponseFromFile("create_user_invalid_identity_type.json"))));

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("IdentityType")
            ? Attribute.create("IdentityType", "invalid_identity_type") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidGenderAndSuccess() {
    wireMockServer.stubFor(post(urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH)).willReturn(
        aResponse().withStatus(201)
            .withBody(TestUtil.getResponseFromFile("create_user_invalid_gender.json"))));

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Gender") ? Attribute.create("Gender", "invalid_gender") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidIntroductionAndSuccess() {
    wireMockServer.stubFor(post(urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH)).willReturn(
        aResponse().withStatus(201)
            .withBody(TestUtil.getResponseFromFile("create_user_with_invalid_introduction.json"))));

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Introduction")
            ? Attribute.create("Introduction", "invalid_introduction") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidCountryAndSuccess() {
    wireMockServer.stubFor(post(urlEqualTo("/" + USERS_ENDPOINT_SUB_PATH)).willReturn(
        aResponse().withStatus(201)
            .withBody(TestUtil.getResponseFromFile("create_user_with_invalid_country.json"))));

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Country") ? Attribute.create("Country", "US.") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithDifferentRdnAndUserName() {
    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("UserName") ? Attribute.create("UserName", "test_user_name1") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidActiveStatus() {
    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("Active") ? Attribute.create("Active", "invalid_active_status") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidLockedOutStatus() {

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("LockedOut")
            ? Attribute.create("LockedOut", "invalid_locked_out_status") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithInvalidInternalIntegrationUserStatus() {

    List<Attribute> attributes = TestUtil.getBaseUserCreationAttributes();
    attributes.replaceAll(attr ->
        attr.getName().equals("InternalIntegrationUser")
            ? Attribute.create("InternalIntegrationUser",
                "invalid_internal_integration_user_status") : attr
    );

    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithSysId() {
    List<Attribute> attributes = TestUtil.getMinimalUserAttributes();
    attributes.add(Attribute.create("sysid", "test_sys_id"));
    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithFederatedId() {
    List<Attribute> attributes = TestUtil.getMinimalUserAttributes();
    attributes.add(Attribute.create("federatedid", "test_federated_id"));
    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateUserWithFullName() {
    List<Attribute> attributes = TestUtil.getMinimalUserAttributes();
    attributes.add(Attribute.create("name", "test_full_name"));
    LdapAddRequest ldapAddRequest = LdapAddRequest.builder()
        .dn(DN.from(USERS_DN))
        .attributes(attributes)
        .build();

    LdapResponse<Void> response = serviceNowConnector.create(ldapAddRequest);
    assertThat(response.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testGetUserWithUserRolesVerifyUserRolesField() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_test_user_name.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).as("UserRoles").containsExactlyInAnyOrder("admin", "user_admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserWithCaretInNameVerifyUserRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user_name=user^^test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("user_with_caret_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=user^^test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_user_with_caret.json"))));

    LdapSearchRequest userWithCaretRequest = LdapSearchRequest.create(
        DN.from("user=user^test,ou=Users,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            userWithCaretRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("user^test", entry.get("UserName"));
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).containsExactlyInAnyOrder("admin^role", "user,admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserWithCommaInNameVerifyUserRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user_name=user,test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("user_with_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=user,test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_user_with_comma.json"))));

    LdapSearchRequest userWithCommaRequest = LdapSearchRequest.create(
        DN.from("user=user\\,test,ou=Users,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            userWithCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("user,test", entry.get("UserName"));
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).containsExactlyInAnyOrder("admin^role", "user,admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserWithCaretAndCommaInNameVerifyUserRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user_name=user^^test,admin"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_with_caret_and_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=user^^test,admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_user_with_caret_and_comma.json"))));

    LdapSearchRequest userWithCaretAndCommaRequest = LdapSearchRequest.create(
        DN.from("user=user^test\\,admin,ou=Users,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            userWithCaretAndCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("user^test,admin", entry.get("UserName"));
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).containsExactlyInAnyOrder("admin^role", "user,admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserWithEmptyUserRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    int usersFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      usersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, usersFetched);
  }

  @Test
  @SneakyThrows
  void testGetMultipleUsersWithDifferentUserRoles() {
    int wiremockPortNo = (int) connectionProperties.get(URL_PORT_KEY);
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("user.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_user.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_test_user_name.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=dana"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_dana.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllUsersLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalUsers = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> user : list) {
        totalUsers++;
        List<String> userRoles = (List<String>) user.get("UserRoles");
        if ("test_user_name".equals(user.get("Username"))) {
          assertThat(userRoles).as("UserRoles").containsExactlyInAnyOrder("admin", "user_admin");
        } else if ("dana".equals(user.get("Username"))) {
          assertThat(userRoles).as("UserRoles").containsExactlyInAnyOrder("itil");
        }
      }
    }
    assertEquals(2, totalUsers);
  }

  @Test
  @SneakyThrows
  void testGetUserUserRolesWithInvalidNextLinkWithFailure() {
    int wiremockPortNo = (int) connectionProperties.get(URL_PORT_KEY);
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_has_role?sysparm_query=user"
                        + ".user_nameINtest_user_name&sysparm_fields=role.name,user"
                        + ".user_name&sysparm_offset=0>;rel=\"first\","
                        + "<invalid-user-roles-url>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("user_roles_batch_page1.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetUserUserRolesWithNextLinkWithSuccess() {
    int wiremockPortNo = (int) connectionProperties.get(URL_PORT_KEY);
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_has_role?sysparm_query=user"
                        + ".user_nameINtest_user_name&sysparm_fields=role.name,user"
                        + ".user_name&sysparm_offset=0&inherited=false>;rel=\"first\","
                        + "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_has_role?sysparm_query=user"
                        + ".user_nameINtest_user_name&sysparm_fields=role.name,user"
                        + ".user_name&sysparm_offset=1&inherited=false>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("user_roles_batch_page1.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_has_role?sysparm_query=user"
            + ".user_nameINtest_user_name&sysparm_fields=role.name,user"
            + ".user_name&sysparm_offset=1&inherited=false"))
            .willReturn(aResponse().withStatus(200).withBody(
                TestUtil.getResponseFromFile("user_roles_batch_page2.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertEquals(4, userRoles.size());
      assertEquals("admin", userRoles.get(0));
      assertEquals("user_admin", userRoles.get(1));
      assertEquals("itil", userRoles.get(2));
      assertEquals("security_admin", userRoles.get(3));
    }
  }

  @Test
  @SneakyThrows
  void testGetUserUserRolesWithInternalServerErrorWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withStatus(500)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetMultipleUsersInSinglePageWithUserRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("multiple_users_single_page.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo(
                "user.user_name=test_user_name^ORuser.user_name=dana^ORuser"
                    + ".user_name=kim"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("multiple_users_roles.json"))));

    ReadOnlyProperties connectionPropertiesWithBatchSize3PageSize3 =
        TestUtil.getConnectionPropertiesWithBatchSize3AndPageSize3();
    HttpClient httpClient = new HttpClient(connectionPropertiesWithBatchSize3PageSize3);

    serviceNowConnector =
        new ServiceNowConnector(targetSchemaObjects, schema,
            connectionPropertiesWithBatchSize3PageSize3, httpClient);

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllUsersLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalUsers = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> user : list) {
        totalUsers++;
        List<String> userRoles = (List<String>) user.get("UserRoles");
        String userName = (String) user.get("UserName");
        if ("test_user_name".equals(userName)) {
          assertThat(userRoles).as("UserRoles for " + userName)
              .containsExactlyInAnyOrder("admin");
        } else if ("dana".equals(userName)) {
          assertThat(userRoles).as("UserRoles for " + userName).containsExactlyInAnyOrder("itil");
        } else if ("kim".equals(userName)) {
          assertThat(userRoles).as("UserRoles for " + userName)
              .containsExactlyInAnyOrder("user_admin");
        }
      }
    }
    assertEquals(3, totalUsers);
  }

  @Test
  @SneakyThrows
  void testGetUserRolesWithUnexpectedUserInResponse() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_unexpected_user.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).containsExactlyInAnyOrder("admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserRolesWithMalformedRows() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_malformed_rows.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).containsExactlyInAnyOrder("admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserWithoutUserRolesInSchema() {
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectForUserWithoutUserRoles()));
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertThat(entry.containsKey("UserRoles")).isFalse();
    }
    wireMockServer.verify(0,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
  }

  @Test
  @SneakyThrows
  void testGetUserWithCaseInsensitiveUserRolesField() {
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectForUserWithDifferentCaseUserRoles()));
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("user_roles_batch_test_user_name.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> userRoles = (List<String>) entry.get("UserRoles");
      assertThat(userRoles).as("UserRoles").containsExactlyInAnyOrder("admin", "user_admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetUserWithRolesWithHostMismatchInNextLink() {
    int wiremockPortNo = (int) connectionProperties.get(URL_PORT_KEY);
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("user.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("user.user_name=test_user_name"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,user.user_name"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://different-host:" + wiremockPortNo
                        + "/api/now/table/sys_user_has_role?sysparm_query=user"
                        + ".user_name=test_user_name&sysparm_fields=role.name,user.user_name"
                        + "&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("user_roles_batch_test_user_name.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleUserLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  private LdapSearchRequest getAllUsersLdapRequest() {
    return LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
        SearchFilter.from("(objectclass=*)"));
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserWithSuccess() {
    stubUserRoleApiWithSuccess("addRolesIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations", "view_changer",
            "sn_udc.basic_read")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(3, "fd_read_operations", "view_changer", "sn_udc.basic_read");
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInMultipleFormatInputWithSuccess() {
    stubUserRoleApiWithSuccess("addRolesMultiFormatIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("[\"fd_read_operations\"]", "view_changer",
            "sn_udc.basic_read")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(3, "fd_read_operations", "view_changer", "sn_udc.basic_read");
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingCommaAsValueInJsonArrayWithIncorrectRoleGotLinked() {
    stubUserRoleApiWithSuccess("addRolesMultiFormatIT", 3);

    // backend does not receive values in inverted comma, else no problem would have been there
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("[fd_read_operations,]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    // below ideally fd_read_operations, should have been linked as against fd_read_operations
    verifyUserRoleApiCalls(1, "fd_read_operations");
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingCommaAsValueWithIncorrectRoleGotLinked() {
    stubUserRoleApiWithSuccess("addRolesMultiFormatIT", 3);

    // backend does not receive values in inverted comma, else no problem would have been there
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations,,view_changer")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    // below ideally fd_read_operations, & view_changer should have been linked as against below
    verifyUserRoleApiCalls(1, "fd_read_operations,,view_changer");
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingCommaAsValueWithCorrectRoleGotLinked() {
    stubUserRoleApiWithSuccess("addRolesMultiFormatIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations,")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(1, "fd_read_operations,");
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingNullAsValueWithFailure() {
    stubUserRoleApiWithSuccess("testAddRolesToUserInContainingNullAsValueWithFailure", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyUserRoleApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingNullAsValueInJsonArrayWithNoRoleToAdd() {
    stubUserRoleApiWithSuccess(
        "testAddRolesToUserInContainingNullAsValueInJsonArrayWithNoRoleToAdd", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("[null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserWithMixedValidRoleAndNullInJsonArray() {
    stubUserRoleApiWithSuccess("testAddRolesToUserWithMixedValidRoleAndNullInJsonArray", 1);

    // Test documented behavior: if ["admin",null] → admin will be linked, null is filtered out
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("[\"admin\",null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    // Verify exactly one API call was made for "admin", null was filtered out
    verifyUserRoleApiCalls(1, "admin");
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingNullAsValueForReplaceModificationTypeWithFailure() {
    stubUserRoleApiWithSuccess(
        "testAddRolesToUserInContainingNullAsValueForReplaceModificationTypeWithFailure", 3);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyUserRoleApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserInContainingNullAsValueInJsonArrayForReplaceWithNoRoleToAdd() {
    stubUserRoleApiWithSuccess(
        "testAddRolesToUserInContainingNullAsValueInJsonArrayForReplaceWithNoRoleToAdd",
        3);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles", Lists.newArrayList("[null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(400)
            .withBody("{\"error\":{\"message\":\"Invalid role name\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations", "view_changer",
            "sn_udc.basic_read")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRolesToUserWithInternalServerError() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations", "view_changer",
            "sn_udc.basic_read")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithReplaceModificationTypeWithJsonArrayInputWithSuccess() {
    stubCommonMocksForDeleteAllUserRoles();
    stubUserRoleApiWithSuccess("replaceRolesJsonArrayIT", 2);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles",
            Lists.newArrayList("[\"fd_read_operations\",\"view_changer\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(2, "fd_read_operations", "view_changer");
  }

  private void stubCommonMocksForDeleteAllUserRoles() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_has_role?sysparm_query=user"
            + ".user_name%3Dtest_user_name&sysparm_fields=role.name%2Cuser"
            + ".user_name&sysparm_limit=1&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"role.name\": \"admin\",\n"
                    + "            \"user.user_name\": \"test_user_name\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")));

    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_has_role?sysparm_query=user.user_name%3Dtest_user_name%5Erole"
            + ".name%3Dadmin&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"sys_id\": \"7bbe2beec3748b1019b4fa0ed401313a\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")));

    wireMockServer.stubFor(delete(urlPathEqualTo(
        "/api/now/table/sys_user_has_role/7bbe2beec3748b1019b4fa0ed401313a"))
        .willReturn(aResponse().withStatus(204)));
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithReplaceModificationTypeWithSuccess() {
    stubCommonMocksForDeleteAllUserRoles();
    stubUserRoleApiWithSuccess("replaceRolesSingleIT", 1);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles",
            Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(1, "fd_read_operations");
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithMultipleValuesWithReplaceModificationTypeWithSuccess() {
    stubCommonMocksForDeleteAllUserRoles();
    stubUserRoleApiWithSuccess(
        "testAddUserRolesWithMultipleValuesWithReplaceModificationTypeWithSuccess", 2);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles",
            Lists.newArrayList("[\"fd_read_operations\",\"fd_read_operations1\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(2, "fd_read_operations", "fd_read_operations1");
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithBlankValuesSkipsBlankAndAddsValidRoles() {
    stubUserRoleApiWithSuccess("testAddUserRolesWithBlankValuesSkipsBlankAndAddsValidRoles", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles",
            Lists.newArrayList("[\"fd_read_operations\",\"\",\" \",\"  \"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyUserRoleApiCalls(1, "fd_read_operations");
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithOnlyBlankValuesWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles",
            Lists.newArrayList("[\"\",\" \",\"  \"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyUserRoleApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithReplaceModificationTypeWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(400)
            .withBody("{\"error\":{\"message\":\"Invalid role assignment\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles",
            Lists.newArrayList("[\"fd_read_operations\",\"view_changer\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUserRolesWithReplaceModificationTypeWithInternalServerError() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("UserRoles",
            Lists.newArrayList("[\"fd_read_operations\",\"view_changer\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRoleToUserWith201ButBlankSysIdWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":{\"sys_id\":\"\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRoleToUserWith201ButNullResultWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":null}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRoleToUserWith201ButEmptyResponseWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"name\":\"test_user_name\"},"
                    + "\"role\":{\"name\":\"fd_read_operations\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_has_role/membership123"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role/membership123")));
  }

  @Test
  @SneakyThrows
  void testDeleteMultipleRolesFromUserWithSuccess() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_has_role?sysparm_query=user.user_name%3Dtest_user_name%5Erole"
            + ".name%3Dfd_read_operations&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"name\":\"test_user_name\"},"
                    + "\"role\":{\"name\":\"fd_read_operations\"}}]}")));

    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_has_role?sysparm_query=user.user_name%3Dtest_user_name%5Erole"
            + ".name%3Dadmin&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership1234\","
                    + "\"user\":{\"name\":\"test_user_name\"},"
                    + "\"role\":{\"name\":\"admin\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo(
        "/api/now/table/sys_user_has_role/membership123"))
        .willReturn(aResponse().withStatus(204)));

    wireMockServer.stubFor(delete(urlPathEqualTo(
        "/api/now/table/sys_user_has_role/membership1234"))
        .willReturn(aResponse().withStatus(204)));


    Modification modification =
        Modification.create(Modification.ModificationType.DELETE,
            Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations", "admin")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(2,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role/membership123")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role/membership1234")));
  }

  @Test
  @SneakyThrows
  void testDeleteAllRolesFromUserWithSuccess() {
    stubCommonMocksForDeleteAllUserRoles();
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(2,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo(
                "/api/now/table/sys_user_has_role/7bbe2beec3748b1019b4fa0ed401313a")));
  }

  @Test
  @SneakyThrows
  void testDeleteAllRolesFromUserWhenUserHasNoRolesWithSuccess() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_has_role?sysparm_query=user"
            + ".user_name%3Dtest_user_name&sysparm_fields=role.name%2Cuser"
            + ".user_name&sysparm_limit=1&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(
            WireMock.urlPathMatching("/api/now/table/sys_user_has_role/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithBlankValuesSkipsBlankAndDeletesValidRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"name\":\"test_user_name\"},"
                    + "\"role\":{\"name\":\"fd_read_operations\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_has_role/membership123"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations", "", " ", "  ")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role/membership123")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithOnlyBlankValuesWithSuccess() {
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("", " ", "  ")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(0,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_has_role/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWhenMembershipNotFoundWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_has_role/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWhenGetMembershipFails() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWhenDeleteFails() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"name\":\"test_user_name\"},"
                    + "\"role\":{\"name\":\"fd_read_operations\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_has_role/membership123"))
        .willReturn(aResponse().withStatus(403)
            .withBody("{\"error\":{\"message\":\"ACL not allowed\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role/membership123")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithNullResponseBodyWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":null}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("fd_read_operations")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithJsonArrayStringWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("[\"admin\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_has_role/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithNullValueWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_has_role"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_has_role/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteRoleFromUserWithBlankValueSkippedWithSuccess() {
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("UserRoles", Lists.newArrayList("")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, USERS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(0,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_has_role")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_has_role/.*")));
  }
}
