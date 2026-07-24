package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.radiantlogic.utility.Constants.CREATE_USER_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_USERS_ENDPOINT;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static com.radiantlogic.utility.Constants.RED_ROCK_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.UPDATE_USER_ENDPOINT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.CyberArkConnector;
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
import com.radiantlogic.util.TestUtils;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class UserIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  CyberArkConnector cyberArkConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;

  static final String CREATE_USER_MOCK_RESPONSE_FILE_NAME = "create_user.json";
  static final String CREATE_USER_DN = "user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk";
  static final String USER_NAME_ATTRIBUTE_KEY = "user-name";
  static final String USER_DISPLAY_NAME_ATTRIBUTE_KEY = "display-name";
  static final String USER_MAIL_ATTRIBUTE_KEY = "user-mail";
  static final String USER_MOBILE_ATTRIBUTE_KEY = "user-mobile";
  static final String USER_NAME_VALUE = "abcc1s232@cyberark.cloud.39470";
  static final String USER_DISPLAY_NAME_VALUE = "test display name";
  static final String USER_EMAIL_VALUE = "abc@gmail.com";
  static final String USER_MOBILE_VALUE = "+91123456789";

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
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForUser()));
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));
  }

  @Test
  @SneakyThrows
  void createUser() {
    wireMockServer.stubFor(post(urlEqualTo("/" + CREATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile(CREATE_USER_MOCK_RESPONSE_FILE_NAME))));

    final LdapAddRequest ldapAddRequest = LdapAddRequest.builder().dn(DN.from(CREATE_USER_DN))
        .attributes(Lists.newArrayList(Attribute.create(USER_NAME_ATTRIBUTE_KEY, USER_NAME_VALUE),
            Attribute.create(USER_DISPLAY_NAME_ATTRIBUTE_KEY, USER_DISPLAY_NAME_VALUE),
            Attribute.create(USER_MOBILE_ATTRIBUTE_KEY, USER_MOBILE_VALUE),
            Attribute.create(USER_MAIL_ATTRIBUTE_KEY, USER_EMAIL_VALUE))).build();

    LdapResponse<?> actual = cyberArkConnector.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateUserDisplayName() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateUserMail() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("user-mail", "a@g.com"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserMail() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("user-mail", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateUserMobile() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("user-mobile", "+91112233"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserMobile() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("user-mobile", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserDisplayName() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("display-name", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserUuid() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("user-id", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void addUserMobileWithTwoMobiles() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("user-mobile", Lists.newArrayList("123", "456")));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateInvalidUser() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_USER_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateUserWithInvalidCredentials() {

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateUserWithInvalidRdnKey() {
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user1=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUser() {
    wireMockServer.stubFor(post(urlEqualTo("/" + DELETE_USERS_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"));

    LdapResponse<String> response = cyberArkConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  void getPaginatedUsers() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedusersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedusersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_user.json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedusersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(objectclass=*)"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalUsersFetched);

  }

  @Test
  void getUserByUsername() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    final LdapSearchRequest ldapSearchRequest = LdapSearchRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"), SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);

  }

  @Test
  @SneakyThrows
  void getUserByUsernameUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByUsernameUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByUsernameUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(name=abcc1s232@cyberark.cloud.39470))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByUsernameUsingFiltersWithSubStringValue() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getUserByUsernameUsingFiltersWithSubStringValueIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getUserByUsernameUsingFiltersWithSubStringValueIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(name=abcc1s232*))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUsersByUsernameUsingFiltersWithMultipleValues() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByUsernameUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByUsernameUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from(
                "(&(name=abcc1s233@cyberark.cloud.39470)(name=abcc1s232@cyberark.cloud.39470))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByIdUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByIdUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByIdUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(id=8d64055b-1234-4550-9dbb-5c8f2bce35db))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByMailUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByMailUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByMailUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(mail=abc@gmail.com))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByRiskLevelRankUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByRiskLevelRankUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByRiskLevelRankUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(risklevelrank=5))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUsersByProvidingPartialRiskLevelRankUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByRiskLevelRankUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByRiskLevelRankUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(risklevelrank=5*))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByMobileUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByMobileUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByMobileUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(mobilenumber=+91123456))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByDisplayNameUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByDisplayNameUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByDisplayNameUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(displayname=abcc1s232))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUsersByProvidingPartialDisplayNameUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByDisplayNameUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByDisplayNameUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(displayname=abcc*))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByStatusUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByStatusUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByStatusUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(status=not invited))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserBySourceUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserBySourceUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserBySourceUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(source=CyberArk Cloud Directory))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserBySourceWithNullValueUsingFilters() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getUserBySourceWithNullValueUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getUserBySourceWithNullValueUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdUser)(source=null))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalUsersFetched);
  }

  @Test
  void getPaginatedUsersWithMultipleFilters() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getPaginatedUsersWithMultipleFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getPaginatedUsersWithMultipleFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_user.json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getPaginatedUsersWithMultipleFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(name=abcc@cyberark.cloud.39470)(mail=abc@example.org))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalUsersFetched);
  }

  @Test
  @SneakyThrows
  void getUserByRiskLevelRankUsingPresenceFilter() {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByRiskLevelRankUsingFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("users.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getUserByRiskLevelRankUsingFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Users"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=*)(risklevelrank=*))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalUsersFetched = 0;
    while (dataProvider.hasMore()) {
      totalUsersFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalUsersFetched);
  }
}