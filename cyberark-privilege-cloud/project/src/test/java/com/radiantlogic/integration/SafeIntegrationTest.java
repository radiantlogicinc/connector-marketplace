package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
class SafeIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  CyberArkConnector cyberArkConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;
  LdapSearchRequest ldapSearchRequestToGetAllSafes =
      LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
          SearchFilter.from("(objectclass=*)"));

  LdapSearchRequest ldapSearchRequestToGetParticularSafe =
      LdapSearchRequest.create(DN.from("safe=test_safe,ou=Safes,ou=CyberArk"), SearchScope.BASE,
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

    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("safe"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForSafe()));

    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));
    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_with_next_link.json"))));
    wireMockServer.stubFor(get(urlPathEqualTo("//API/Safes/")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));
    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));
  }


  @Test
  void getPaginatedSafesWithNoMembers() throws JsonProcessingException {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe/Members")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllSafes);
    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalSafesFetched);
  }

  @Test
  void getPaginatedSafesWithMembers() throws JsonProcessingException {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe/Members")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safe_members_with_next_link.json"))));

    wireMockServer.stubFor(get(urlPathEqualTo("//API/Safes/test_safe/Members")).willReturn(
        aResponse().withBody(
            TestUtils.getResponseFromFile("safe_member_without_next_link.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllSafes);
    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalSafesFetched);
  }

  @Test
  void getSafeWithMembers() throws JsonProcessingException {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safe.json"))));

    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe/Members")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("single_safe_member.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularSafe);
    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  void getSafeWithNoMembers() throws JsonProcessingException {
    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safe.json"))));

    wireMockServer.stubFor(get(urlPathEqualTo("/API/safes/test_safe/Members")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetParticularSafe);
    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafeByProvidingExactNameWithFilters() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false&search=test_safe1")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdSafe)(name=test_safe1))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafesByProvidingExactNameWithFiltersWithMultipleValues() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(name=test_safe2)(name=test_safe1))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafesByProvidingMultipleFilters() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(number=16)(name=test_safe1))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafesByUsingNotAllowedFilterColumn() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(number=16)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafesByProvidingPartialNameWithFilters() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false&search=safe1")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdSafe)(name=safe1))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }

    /*
     * here as when providing value, * not used, iddm will treat as looking for exact value
     * 'safe1', so will discard this record
     */

    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafesByProvidingPartialNameWithAsteriskWithFilters() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    /*
        * as get safes api doesn't support * based filtering, so no filter will be applied in
          this case
        * here backend will send back all safes, iddm will filter out
     */

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdSafe)(name=*safe1))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  @SneakyThrows
  void getSafeByNameHavingNullValueWithFilters() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdSafe)(name=null))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }

  @Test
  void getPaginatedSafesWithoutSubdomain() {
    connectionProperties = TestUtils.getConnectionPropertiesWithoutSubDomain();
    cyberArkConnector = new CyberArkConnector(targetSchemaObjects, schema, connectionProperties);

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetAllSafes);

    LdapResponse<String> response = dataProvider.next();
    assertEquals(LdapResultCode.UNAVAILABLE, response.getStatus());
  }

  @Test
  @SneakyThrows
  void getSafesByProvidingUsingPresenceFilters() {

    wireMockServer.stubFor(get(urlEqualTo(
        "/API/safes?limit=100&sort=safeName&useCache=false")).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("safes_without_next_link.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/API/safes/test_safe1/Members")).willReturn(
            aResponse().withBody(TestUtils.getResponseFromFile("empty_safe_members.json"))));

    LdapSearchRequest ldapSearchRequestToGetSafeByName =
        LdapSearchRequest.create(DN.from("ou=Safes"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=*)(name=*))"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(
            ldapSearchRequestToGetSafeByName);

    int totalSafesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      totalSafesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalSafesFetched);
  }
}