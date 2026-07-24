package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_KEY;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
class DirectoryServiceIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  CyberArkConnector cyberArkConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;
  LdapSearchRequest ldapSearchRequestToGetAllDirectoryServices =
      LdapSearchRequest.create(DN.from("ou=DS"), SearchScope.SUB,
          SearchFilter.from("(objectclass=*)"));

  LdapSearchRequest ldapSearchRequestToGetParticularDirectoryService =
      LdapSearchRequest.create(DN.from("service=CDS,ou=DS,ou=CyberArk"), SearchScope.BASE,
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
        Collections.singletonList(DIRECTORY_SERVICES_KEY));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtils.getSchemaObjectForDirectoryService()));

    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));
  }


  @Test
  @SneakyThrows
  void getDirectoryServices() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("directory_services.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllDirectoryServices);
    int totalDirectoryServicesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalDirectoryServicesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalDirectoryServicesFetched);
  }

  @Test
  @SneakyThrows
  void getDirectoryServiceByName() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("directory_services.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularDirectoryService);
    int totalDirectoryServicesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalDirectoryServicesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalDirectoryServicesFetched);
  }

  @Test
  void getDirectoryServicesWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withStatus(400)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllDirectoryServices);

    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  void getDirectoryServicesWithInvalidCredentials() {
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(400)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllDirectoryServices);

    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getDirectoryServiceByNameWithInCompleteResponse() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(
            TestUtils.getResponseFromFile("directory_services_with_null_response.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularDirectoryService);

    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getDirectoryServiceFilteredByName() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("directory_services.json"))));

    LdapSearchRequest ldapSearchRequestToGetDirectoryServiceFilteredByName =
        LdapSearchRequest.create(DN.from("ou=DS"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vddirectoryservice)(name=RL_CArk))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetDirectoryServiceFilteredByName);
    int totalDirectoryServicesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalDirectoryServicesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalDirectoryServicesFetched);
  }

  @Test
  @SneakyThrows
  void getDirectoryServiceFilteredByNameWithMultipleValues() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("directory_services.json"))));

    LdapSearchRequest ldapSearchRequestToGetDirectoryServiceFilteredByName =
        LdapSearchRequest.create(DN.from("ou=DS"), SearchScope.SUB,
            SearchFilter.from("(&(name=RL_CArk1)(name=RL_CArk))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetDirectoryServiceFilteredByName);
    int totalDirectoryServicesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalDirectoryServicesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalDirectoryServicesFetched);
  }

  @Test
  @SneakyThrows
  void getDirectoryServicesUsingMultipleFilters() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("directory_services.json"))));

    LdapSearchRequest ldapSearchRequestToGetDirectoryServiceFilteredByName =
        LdapSearchRequest.create(DN.from("ou=DS"), SearchScope.SUB,
            SearchFilter.from("(&(config=test)(name=RL_CArk))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetDirectoryServiceFilteredByName);
    int totalDirectoryServicesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalDirectoryServicesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalDirectoryServicesFetched);
  }

  @Test
  @SneakyThrows
  void getDirectoryServicesUsingNotAllowedFilterColumn() {
    wireMockServer.stubFor(post(urlPathEqualTo("/core/GetDirectoryServices")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("directory_services.json"))));

    LdapSearchRequest ldapSearchRequestToGetDirectoryServiceFilteredByName =
        LdapSearchRequest.create(DN.from("ou=DS"), SearchScope.SUB,
            SearchFilter.from("(config=test)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetDirectoryServiceFilteredByName);
    int totalDirectoryServicesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalDirectoryServicesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalDirectoryServicesFetched);
  }
}