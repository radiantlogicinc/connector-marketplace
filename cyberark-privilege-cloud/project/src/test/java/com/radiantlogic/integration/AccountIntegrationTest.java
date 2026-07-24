package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.radiantlogic.CyberArkConnector;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.util.TestUtils;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class AccountIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  CyberArkConnector cyberArkConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;
  LdapSearchRequest ldapSearchRequestToGetAllAccounts =
      LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
          SearchFilter.from("(objectclass=*)"));

  LdapSearchRequest ldapSearchRequestToGetParticularAccount =
      LdapSearchRequest.create(DN.from("safe=16_4,ou=Accounts,ou=CyberArk"), SearchScope.BASE,
          SearchFilter.from("(objectclass=*)"));

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

    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(
        Collections.singletonList("account"));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtils.getSchemaObjectForAccount()));

    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));
  }


  @Test
  void getPaginatedAccounts() throws JsonProcessingException {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/Accounts")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("accounts_with_next_link.json"))));

    wireMockServer.stubFor(get(urlPathEqualTo("//API/Accounts")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("accounts_without_next_link.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllAccounts);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }

  @Test
  void getAccountById() throws JsonProcessingException {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/Accounts/16_4")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("account.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularAccount);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalAccountsFetched);
  }

  @Test
  void getDeletedAccountById() {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/Accounts/16_4")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("account_with_deletion_time.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularAccount);
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  void getDeletedAccountByIdWithBadRequest() {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/Accounts/16_4")).willReturn(
        aResponse().withStatus(400)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularAccount);
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  void getDeletedAccountByIdWithInvalidCredentials() {
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(400)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularAccount);
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  private void stubCommonMocks() {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/Accounts")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("accounts_with_next_link.json"))));

    wireMockServer.stubFor(get(urlPathEqualTo("//API/Accounts")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("accounts_without_next_link.json"))));
  }

  @Test
  @SneakyThrows
  void getAccountsFilteredBySafeName() {
    stubCommonMocks();
    LdapSearchRequest ldapSearchRequestToGetAccountsFilteredBySafeName =
        LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdAccount)(safename=another_test_safe))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAccountsFilteredBySafeName);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }

  @Test
  @SneakyThrows
  void getAccountsFilteredBySafeNameUsingMultipleValues() {
    stubCommonMocks();
    LdapSearchRequest ldapSearchRequestToGetAccountsFilteredBySafeName =
        LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
            SearchFilter.from("(&(safename=another_test_safe1)(safename=another_test_safe))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAccountsFilteredBySafeName);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }

  @Test
  @SneakyThrows
  void getAccountsUsingMultipleFilters() {
    stubCommonMocks();
    LdapSearchRequest ldapSearchRequestToGetAccountsFilteredBySafeName =
        LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
            SearchFilter.from("(&(id=1)(safename=another_test_safe))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAccountsFilteredBySafeName);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }

  @Test
  @SneakyThrows
  void getAccountsUsingNotAllowedFilterColumn() {
    stubCommonMocks();
    LdapSearchRequest ldapSearchRequestToGetAccountsFilteredBySafeName =
        LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
            SearchFilter.from("(id=1)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAccountsFilteredBySafeName);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }

  @Test
  @SneakyThrows
  void getAccountsFilteredBySafeNameWithPartialValueProvidedUsingAsterisk() {
    stubCommonMocks();
    /*
        * as get accounts api doesn't support * based filtering, so no filter will be applied in
          this case
        * here backend will send back all accounts, iddm will filter out
     */

    LdapSearchRequest ldapSearchRequestToGetAccountsFilteredBySafeName =
        LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdAccount)(safename=*test_safe))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAccountsFilteredBySafeName);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }

  @Test
  void getPaginatedAccountsWithoutSubdomain() {
    connectionProperties = TestUtils.getConnectionPropertiesWithoutSubDomain();
    cyberArkConnector = new CyberArkConnector(targetSchemaObjects, schema, connectionProperties);

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllAccounts);

    LdapResponse<String> response = dataProvider.next();
    assertEquals(LdapResultCode.UNAVAILABLE, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getAccountsUsingPresenceFilter() {
    stubCommonMocks();
    /*
        * as get accounts api doesn't support * based filtering, so no filter will be applied in
          this case
        * here backend will send back all accounts, iddm will filter out
     */

    LdapSearchRequest ldapSearchRequestToGetAccountsFilteredBySafeName =
        LdapSearchRequest.create(DN.from("ou=Accounts"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=*)(safename=*))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAccountsFilteredBySafeName);
    int totalAccountsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalAccountsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalAccountsFetched);
  }
}