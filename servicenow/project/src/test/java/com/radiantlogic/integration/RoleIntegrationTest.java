package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.radiantlogic.iddm.ldap.LdapResultCode.OPERATIONS_ERROR;
import static com.radiantlogic.iddm.ldap.LdapResultCode.SUCCESS;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.radiantlogic.ServiceNowConnector;
import com.radiantlogic.http.HttpClient;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.base.SearchScope;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.util.TestUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleIntegrationTest {

  private static final String ROLES_DN = "role=test_parent_role,ou=Roles,ou=ServiceNow";

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  ServiceNowConnector serviceNowConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;
  private int wiremockPortNo;

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
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(
        Collections.singletonList("role"));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectForRole()));
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));
    wiremockPortNo = (int) connectionProperties.get(URL_PORT_KEY);
  }

  @Test
  @SneakyThrows
  void testGetRoleWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("role_delegator", entry.get("Name"));
      assertEquals("Can delegate roles to group members", entry.get("Description"));
      assertEquals("011ba5aa0a0a0b3001a439c580549134", entry.get("SysId"));
      assertEquals("false", entry.get("Grantable"));
      assertEquals("false", entry.get("ElevatedPrivilege"));
      assertEquals("admin", entry.get("Suffix"));
      assertEquals("false", entry.get("ScopedAdmin"));
      assertEquals("false", entry.get("CanDelegate"));
      assertEquals("user_admin", entry.get("IncludesRoles"));
      assertEquals("XolTkTIC/bmoG0WmwP0UNGHbAE+8GExntslIgs81Bwg=", entry.get("FederatedId"));
      assertEquals("quinn", entry.get("SysCreatedBy"));
      assertEquals("read", entry.get("SysPolicy"));
      rolesFetched += list.size();
    }
    assertEquals(1, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetAllRolesWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        rolesFetched++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        assertThat(childRoles).as("ChildRoles for role")
            .containsExactlyInAnyOrder("admin", "user_admin");
      }
    }
    assertEquals(1, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetRoleContainingCaretSymbolWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));

    LdapSearchRequest singleRoleLdapRequest = LdapSearchRequest.create(
        DN.from("role=role^1,ou=Roles,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleRoleLdapRequest);

    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        rolesFetched++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        assertThat(childRoles).as("ChildRoles for role")
            .containsExactlyInAnyOrder("admin", "user_admin");
      }
    }
    assertEquals(1, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetAllRolesWithEmptyResponseWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("empty_roles.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      rolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(0, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetPaginatedRolesWithSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_role?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_role?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_admin.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      rolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetRoleWithBadRequestWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withStatus(400)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetRoleWithServerErrorWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withStatus(500)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetRoleWithInvalidCredentialsWithFailure() {
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(401)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetRoleWithRetryWithSuccess() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role")).inScenario("getRoleWithRetryIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(429))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role")).inScenario("getRoleWithRetryIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        rolesFetched++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        assertThat(childRoles).as("ChildRoles for role")
            .containsExactlyInAnyOrder("admin", "user_admin");
      }
    }
    assertEquals(1, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetRoleWithRetryUsingRetryAfterHeaderWithSuccess() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role")).inScenario("getRoleWithRetryIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(429).withHeader("Retry-After", "1"))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role")).inScenario("getRoleWithRetryIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        rolesFetched++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        assertThat(childRoles).as("ChildRoles for role")
            .containsExactlyInAnyOrder("admin", "user_admin");
      }
    }
    assertEquals(1, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetRolesWithInvalidNextLinkWithFailure() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_role?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<invalid-url>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("role.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());

    dataProvider.next();
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetRoleWithChildRolesVerifyChildRolesField() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> childRoles = (List<String>) entry.get("ChildRoles");
      assertEquals(2, childRoles.size());
      assertEquals("admin", childRoles.get(0));
      assertEquals("user_admin", childRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetRoleWithCaretInNameVerifyChildRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=role^^test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("role_with_caret_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role^^test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_with_caret.json"))));

    LdapSearchRequest roleWithCaretRequest = LdapSearchRequest.create(
        DN.from("role=role^test,ou=Roles,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            roleWithCaretRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("role^test", entry.get("Name"));
      List<String> childRoles = (List<String>) entry.get("ChildRoles");
      assertEquals(2, childRoles.size());
      assertEquals("admin^role", childRoles.get(0));
      assertEquals("user,admin", childRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetRoleWithCommaInNameVerifyChildRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=role,test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("role_with_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role,test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_with_comma.json"))));

    LdapSearchRequest roleWithCommaRequest = LdapSearchRequest.create(
        DN.from("role=role\\,test,ou=Roles,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            roleWithCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("role,test", entry.get("Name"));
      List<String> childRoles = (List<String>) entry.get("ChildRoles");
      assertEquals(2, childRoles.size());
      assertEquals("admin^role", childRoles.get(0));
      assertEquals("user,admin", childRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetRoleWithCaretAndCommaInNameVerifyChildRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=role^^test,admin"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("role_with_caret_and_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role^^test,admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_with_caret_and_comma.json"))));

    LdapSearchRequest roleWithCaretAndCommaRequest = LdapSearchRequest.create(
        DN.from("role=role^test\\,admin,ou=Roles,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            roleWithCaretAndCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("role^test,admin", entry.get("Name"));
      List<String> childRoles = (List<String>) entry.get("ChildRoles");
      assertEquals(2, childRoles.size());
      assertEquals("admin^role", childRoles.get(0));
      assertEquals("user,admin", childRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetRoleWithEmptyChildRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    int rolesFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        rolesFetched++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        assertThat(childRoles).as("ChildRoles for role").isEmpty();
      }
    }
    assertEquals(1, rolesFetched);
  }

  @Test
  @SneakyThrows
  void testGetMultipleRolesWithDifferentChildRoles() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_role?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_role?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_role.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_role_delegator.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_admin.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalRoles = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        totalRoles++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        if ("role_delegator".equals(role.get("Name"))) {
          assertEquals(2, childRoles.size());
        } else if ("admin".equals(role.get("Name"))) {
          assertEquals(1, childRoles.size());
          assertEquals("itil", childRoles.get(0));
        }
      }
    }
    assertEquals(2, totalRoles);
  }

  @Test
  @SneakyThrows
  void testGetRoleChildRolesWithInvalidNextLinkWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role_contains?sysparm_query=role"
                        + ".nameINrole_delegator&sysparm_fields=role.name,contains"
                        + ".name&sysparm_limit=1&sysparm_offset=0>;rel=\"first\","
                        + "<invalid-child-url>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("child_roles_batch_page1.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetRoleChildRolesWithNextLinkWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role_contains?sysparm_query=role"
                        + ".nameINrole_delegator&sysparm_fields=role.name,contains"
                        + ".name&sysparm_limit=1&sysparm_offset=0>;rel=\"first\","
                        + "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role_contains?sysparm_query=role"
                        + ".nameINrole_delegator&sysparm_fields=role.name,contains"
                        + ".name&sysparm_limit=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("child_roles_batch_page1.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".nameINrole_delegator&sysparm_fields=role.name,contains"
            + ".name&sysparm_limit=1&sysparm_offset=1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_page2.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> childRoles = (List<String>) entry.get("ChildRoles");
      assertEquals(4, childRoles.size());
      assertEquals("admin", childRoles.get(0));
      assertEquals("user_admin", childRoles.get(1));
      assertEquals("itil", childRoles.get(2));
      assertEquals("security_admin", childRoles.get(3));
    }
    wireMockServer.verify(
        WireMock.getRequestedFor(WireMock.urlEqualTo(
            "/api/now/table/sys_user_role_contains?sysparm_query=role"
                + ".nameINrole_delegator&sysparm_fields=role.name,contains"
                + ".name&sysparm_limit=1&sysparm_offset=1")));
  }

  @Test
  @SneakyThrows
  void testGetPaginatedRolesWithPaginatedChildRolesWithSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_role?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_role?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_role.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role_contains?sysparm_query=role"
                        + ".nameINrole_delegator&sysparm_fields=role.name,contains"
                        + ".name&sysparm_limit=1&sysparm_offset=0>;rel=\"first\","
                        + "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_role_contains?sysparm_query=role"
                        + ".nameINrole_delegator&sysparm_fields=role.name,contains"
                        + ".name&sysparm_limit=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("child_roles_batch_page1.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".nameINrole_delegator&sysparm_fields=role.name,contains"
            + ".name&sysparm_limit=1&sysparm_offset=1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_page2.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("child_roles_batch_admin.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalRoles = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        totalRoles++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        if ("role_delegator".equals(role.get("Name"))) {
          assertEquals(4, childRoles.size());
          assertEquals("admin", childRoles.get(0));
          assertEquals("user_admin", childRoles.get(1));
          assertEquals("itil", childRoles.get(2));
          assertEquals("security_admin", childRoles.get(3));
        } else if ("admin".equals(role.get("Name"))) {
          assertEquals(1, childRoles.size());
          assertEquals("itil", childRoles.get(0));
        }
      }
    }
    assertEquals(2, totalRoles);
  }

  @Test
  @SneakyThrows
  void testGetSinglePageWithMultipleParentRolesExceedingBatchSize() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("three_roles_single_page.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo(
                "role.name=parent_role_1^ORrole.name=parent_role_2^ORrole.name=parent_role_3"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("multiple_roles_child_roles.json"))));

    ReadOnlyProperties connectionPropertiesWithBatchSize3PageSize3 =
        TestUtil.getConnectionPropertiesWithBatchSize3AndPageSize3();
    HttpClient httpClient = new HttpClient(connectionPropertiesWithBatchSize3PageSize3);

    serviceNowConnector =
        new ServiceNowConnector(targetSchemaObjects, schema,
            connectionPropertiesWithBatchSize3PageSize3, httpClient);

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllRolesLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalRoles = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> role : list) {
        totalRoles++;
        List<String> childRoles = (List<String>) role.get("ChildRoles");
        String roleName = (String) role.get("Name");
        if ("parent_role_1".equals(roleName)) {
          assertEquals(1, childRoles.size());
          assertEquals("child_a", childRoles.get(0));
        } else if ("parent_role_2".equals(roleName)) {
          assertEquals(1, childRoles.size());
          assertEquals("child_c", childRoles.get(0));
        } else if ("parent_role_3".equals(roleName)) {
          assertEquals(1, childRoles.size());
          assertEquals("child_f", childRoles.get(0));
        }
      }
    }
    assertEquals(3, totalRoles);
  }

  @Test
  @SneakyThrows
  void testGetRoleChildRolesWithInternalServerErrorWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withQueryParam("sysparm_query", WireMock.equalTo("role.name=role_delegator"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,contains.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withStatus(500)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleRoleLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleWithSuccess() {
    stubRoleChildApiWithSuccess("addChildRolesIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles",
            Lists.newArrayList("child_role1", "child_role2", "child_role3")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(3, "child_role1", "child_role2", "child_role3");
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleInMultipleFormatInputWithSuccess() {
    stubRoleChildApiWithSuccess("addChildRolesMultiFormatIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"child_role1\"]", "child_role2", "child_role3")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(3, "child_role1", "child_role2", "child_role3");
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleWithMalformedJsonArrayExtractsFirstToken() {
    stubRoleChildApiWithSuccess("addChildRolesCommaIT", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("[child_role1,]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(1, "child_role1");
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleWithCommaDelimitedValuePreservesRawInput() {
    stubRoleChildApiWithSuccess("addChildRolesCommaIT2", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1,,child_role2")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(1, "child_role1,,child_role2");
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleInContainingCommaAsValueWithCorrectRoleGotLinked() {
    stubRoleChildApiWithSuccess("addChildRolesCommaIT3", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1,")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(1, "child_role1,");
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleInContainingNullAsValueWithFailure() {

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyRoleChildApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleInContainingNullAsValueInJsonArrayWithNoRoleToAdd() {

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("[null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyRoleChildApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleWithMixedValidRoleAndNullInJsonArray() {
    stubRoleChildApiWithSuccess("testAddChildRolesToRoleWithMixedValidRoleAndNullInJsonArray", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("[\"test_child_role\",null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(1, "test_child_role");
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleInContainingNullAsValueForReplaceModificationTypeWithFailure() {

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyRoleChildApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleInContainingNullAsValueInJsonArrayForReplaceWithNoRoleToAdd() {

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles", Lists.newArrayList("[null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyRoleChildApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(400)
            .withBody("{\"error\":{\"message\":\"Bad request\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddChildRolesToRoleWithInternalServerError() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddChildRolesNonTransactionalMidBatchFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":{\"sys_id\":\"membership_success\"}}")));

    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .withRequestBody(WireMock.containing("\"contains\":\"child_role3\""))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"child_role1\",\"child_role2\",\"child_role3\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.postRequestedFor(
                WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withRequestBody(WireMock.containing("\"contains\":\"child_role1\"")));

    wireMockServer.verify(1,
        WireMock.postRequestedFor(
                WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withRequestBody(WireMock.containing("\"contains\":\"child_role2\"")));

    wireMockServer.verify(1,
        WireMock.postRequestedFor(
                WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .withRequestBody(WireMock.containing("\"contains\":\"child_role3\"")));
  }

  @Test
  @SneakyThrows
  void testAddChildRolesFailFastOnFirstBlankSysId() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":{\"sys_id\":\"\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"child_role1\",\"child_role2\",\"child_role3\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.postRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithReplaceModificationTypeWithJsonArrayInputWithSuccess() {
    stubCommonMocksForDeleteAllChildRolesFromRole();
    stubRoleChildApiWithSuccess("replaceChildRolesJsonArrayIT", 2);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"child_role1\",\"child_role2\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(2, "child_role1", "child_role2");
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithReplaceModificationTypeWithSuccess() {
    stubCommonMocksForDeleteAllChildRolesFromRole();
    stubRoleChildApiWithSuccess("replaceChildRolesSingleIT", 1);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles",
            Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(1, "child_role1");
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithReplaceModificationTypeWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(400)
            .withBody("{\"error\":{\"message\":\"Bad request\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithReplaceModificationTypeWithInternalServerError() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithMultipleValuesWithReplaceModificationTypeWithSuccess() {
    stubCommonMocksForDeleteAllChildRolesFromRole();
    stubRoleChildApiWithSuccess(
        "testAddRoleChildRolesWithMultipleValuesWithReplaceModificationTypeWithSuccess", 2);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"child_role1\",\"child_role2\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(2, "child_role1", "child_role2");
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithBlankValuesSkipsBlankAndAddsValidRoles() {
    stubRoleChildApiWithSuccess("testAddRoleChildRolesWithBlankValuesSkipsBlankAndAddsValidRoles",
        1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"child_role1\",\"\",\" \",\"  \"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyRoleChildApiCalls(1, "child_role1");
  }

  @Test
  @SneakyThrows
  void testAddRoleChildRolesWithOnlyBlankValuesWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles",
            Lists.newArrayList("[\"\",\" \",\"  \"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyRoleChildApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddChildRoleToRoleWith201ButBlankSysIdWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":{\"sys_id\":\"\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddChildRoleToRoleWith201ButNullResultWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":null}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddChildRoleToRoleWith201ButEmptyResponseWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"abc123\"}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_role_contains/abc123"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains/abc123")));
  }

  @Test
  @SneakyThrows
  void testDeleteMultipleChildRolesFromRoleWithSuccess() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".name%3Dtest_parent_role%5Econtains"
            + ".name%3Dchild_role1&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membershipId1\"}]}")));

    wireMockServer.stubFor(
        delete(urlPathEqualTo("/api/now/table/sys_user_role_contains/membershipId1"))
            .willReturn(aResponse().withStatus(204)));

    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".name%3Dtest_parent_role%5Econtains"
            + ".name%3Dchild_role2&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membershipId2\"}]}")));

    wireMockServer.stubFor(
        delete(urlPathEqualTo("/api/now/table/sys_user_role_contains/membershipId2"))
            .willReturn(aResponse().withStatus(204)));

    Modification modification =
        Modification.create(Modification.ModificationType.DELETE,
            Attribute.create("ChildRoles", Lists.newArrayList("child_role1", "child_role2")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(2,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo(
            "/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains/membershipId1")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains/membershipId2")));
  }

  private void stubCommonMocksForDeleteAllChildRolesFromRole() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".name%3Dtest_parent_role&sysparm_fields=role.name%2Ccontains"
            + ".name&sysparm_limit=1&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"role.name\": \"test_parent_role\",\n"
                    + "            \"contains.name\": \"admin\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")));

    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".name%3Dtest_parent_role%5Econtains"
            + ".name%3Dadmin&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"sys_id\": \"2ee184b6c3388b1019b4fa0ed4013139\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")));


    wireMockServer.stubFor(delete(
        urlPathEqualTo("/api/now/table/sys_user_role_contains/2ee184b6c3388b1019b4fa0ed4013139"))
        .willReturn(aResponse().withStatus(204)));
  }

  @Test
  @SneakyThrows
  void testDeleteAllChildRolesFromRoleWithSuccess() {
    stubCommonMocksForDeleteAllChildRolesFromRole();
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(2,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo(
            "/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo(
                "/api/now/table/sys_user_role_contains/2ee184b6c3388b1019b4fa0ed4013139")));
  }

  @Test
  @SneakyThrows
  void testDeleteAllChildRolesFromRoleWhenRoleHasNoChildRolesWithSuccess() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_role_contains?sysparm_query=role"
            + ".name%3Dtest_parent_role&sysparm_fields=role.name%2Ccontains"
            + ".name&sysparm_limit=1&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(
            WireMock.urlPathMatching("/api/now/table/sys_user_role_contains/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWithBlankValuesSkipsBlankAndDeletesValidRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"abc123\"}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_role_contains/abc123"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1", "", " ", "  ")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains/abc123")));
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWithOnlyBlankValuesWithSuccess() {
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("", " ", "  ")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(0,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(
            WireMock.urlMatching("/api/now/table/sys_user_role_contains/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWhenMembershipNotFoundWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWhenGetMembershipFails() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWhenDeleteFails() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"abc123\"}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_role_contains/abc123"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains/abc123")));
  }

  @Test
  @SneakyThrows
  void testDeleteChildRoleFromRoleWithNullResponseBodyWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":null}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("ChildRoles", Lists.newArrayList("child_role1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));
  }

  @Test
  @SneakyThrows
  void testModifyRoleWithInvalidAttributeWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("InvalidAttribute", Lists.newArrayList("value1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testModifyRoleWithUnsupportedAttributeWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("Name", Lists.newArrayList("new_role_name")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, ROLES_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  // ==================== Helper Methods ====================

  private LdapModifyRequest getModifyRequest(Modification modification, String dn) {
    return LdapModifyRequest.builder()
        .dn(DN.from(dn))
        .modifications(Collections.singletonList(modification))
        .build();
  }

  private void stubRoleChildApiWithSuccess(String scenarioName, int numberOfChildRoles) {
    for (int i = 0; i < numberOfChildRoles; i++) {
      String currentState = (i == 0) ? Scenario.STARTED : "CALL_" + i;
      String nextState = (i == numberOfChildRoles - 1) ? null : "CALL_" + (i + 1);
      String sysId = String.format("membership_%s_%03d", scenarioName, i);

      if (nextState != null) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(currentState)
            .willReturn(aResponse().withStatus(201)
                .withBody("{\"result\":{\"sys_id\":\"" + sysId + "\"}}"))
            .willSetStateTo(nextState));
      } else {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_role_contains"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(currentState)
            .willReturn(aResponse().withStatus(201)
                .withBody("{\"result\":{\"sys_id\":\"" + sysId + "\"}}")));
      }
    }
  }

  private void verifyRoleChildApiCalls(int expectedCount, String... childRoleNames) {
    wireMockServer.verify(expectedCount,
        WireMock.postRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains")));

    for (String childRoleName : childRoleNames) {
      wireMockServer.verify(1,
          WireMock.postRequestedFor(
                  WireMock.urlPathEqualTo("/api/now/table/sys_user_role_contains"))
              .withRequestBody(WireMock.containing("\"contains\":\"" + childRoleName + "\"")));
    }
  }

  private LdapSearchRequest getSingleRoleLdapRequest() {
    return LdapSearchRequest.create(
        DN.from("role=role_delegator,ou=Roles,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

  private LdapSearchRequest getAllRolesLdapRequest() {
    return LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
        SearchFilter.from("(objectclass=*)"));
  }
}
