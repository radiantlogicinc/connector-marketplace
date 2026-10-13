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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroupIntegrationTest {

  private static final String GROUPS_DN = "group=test_group_name,ou=Groups,ou=ServiceNow";

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
        Collections.singletonList("group"));
    when(schema.getSchemaObject(any())).thenReturn(
        Optional.of(TestUtil.getSchemaObjectForGroup()));
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("access_token.json"))));
    wiremockPortNo = (int) connectionProperties.get(URL_PORT_KEY);
  }

  @Test
  @SneakyThrows
  void testGetGroupWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("MIF Admins", entry.get("Name"));
      assertEquals("test desc", entry.get("Description"));
      assertEquals("true", entry.get("Active"));
      assertEquals("0762d92db72422103a858bbb4e11a928", entry.get("SysId"));
      assertEquals("test source", entry.get("Source"));
      assertEquals("458de4f067671300dbfdbb2d07415ad6", entry.get("Type"));
      assertEquals("false", entry.get("IncludeMembers"));
      assertEquals("admin", entry.get("SysCreatedBy"));
      groupsFetched += list.size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetAllGroupsWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetGroupContainingCaretSymbolWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group_with_caret.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest singleGroupLdapRequest = LdapSearchRequest.create(
        DN.from("group=group^1,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            singleGroupLdapRequest);

    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetEmptyGroupsResponseWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("empty_groups.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(0, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetPaginatedGroupsWithSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_group?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_group?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetGroupWithBadRequestWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withStatus(400)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupWithServerErrorWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withStatus(500)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupWithInvalidCredentialsWithFailure() {
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withStatus(401)));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupWithRetryWithSuccess() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group")).inScenario("getGroupWithRetryIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(429))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group")).inScenario("getGroupWithRetryIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetGroupWithRetryUsingRetryAfterHeaderWithSuccess() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group")).inScenario("getGroupWithRetryIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withStatus(429).withHeader("Retry-After", "1"))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group")).inScenario("getGroupWithRetryIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetGroupsWithInvalidNextLinkWithFailure() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_group?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<invalid-url>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());

    dataProvider.next();
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupWithGroupUsersVerifyGroupUsersField() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_mif_admins.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> groupUsers = (List<String>) entry.get("GroupUsers");
      assertNotNull(groupUsers);
      assertThat(groupUsers).containsExactlyInAnyOrder("itil", "approver");
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithCaretInNameVerifyGroupUsersField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=group^^test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("group_with_caret_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_group_with_caret.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest groupWithCaretRequest = LdapSearchRequest.create(
        DN.from("group=group^test,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            groupWithCaretRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("group^test", entry.get("Name"));
      List<String> groupUsers = (List<String>) entry.get("GroupUsers");
      assertNotNull(groupUsers);
      assertThat(groupUsers).containsExactlyInAnyOrder("user^admin", "user,test");
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithCommaInNameVerifyGroupUsersField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=group,test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("group_with_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group,test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_group_with_comma.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group,test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest groupWithCommaRequest = LdapSearchRequest.create(
        DN.from("group=group\\,test,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            groupWithCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("group,test", entry.get("Name"));
      List<String> groupUsers = (List<String>) entry.get("GroupUsers");
      assertNotNull(groupUsers);
      assertThat(groupUsers).containsExactlyInAnyOrder("user^admin", "user,test");
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithCaretAndCommaInNameVerifyGroupUsersField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=group^^test,admin"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_with_caret_and_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test,admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile(
                    "group_users_batch_group_with_caret_and_comma.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test,admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    LdapSearchRequest groupWithCaretAndCommaRequest = LdapSearchRequest.create(
        DN.from("group=group^test\\,admin,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            groupWithCaretAndCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("group^test,admin", entry.get("Name"));
      List<String> groupUsers = (List<String>) entry.get("GroupUsers");
      assertNotNull(groupUsers);
      assertThat(groupUsers).containsExactlyInAnyOrder("user^admin", "user,test");
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithEmptyGroupUsers() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetMultipleGroupsWithDifferentGroupUsers() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_group?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_group?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_mif_admins.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_database.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalGroups = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> group : list) {
        totalGroups++;
        List<String> groupUsers = (List<String>) group.get("GroupUsers");
        String groupName = (String) group.get("Name");
        if ("MIF Admins".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName).hasSize(2);
        } else if ("Database".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName)
              .containsExactlyInAnyOrder("ATF.User");
        }
      }
    }
    assertEquals(2, totalGroups);
  }

  @Test
  @SneakyThrows
  void testGetGroupUsersWithInvalidNextLinkWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_grmember?sysparm_query=group"
                        + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
                        + ".name&sysparm_limit=1&sysparm_offset=0>;rel=\"first\","
                        + "<invalid-group-user-url>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group_users_batch_page1.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupGroupUsersWithNextLinkWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_grmember?sysparm_query=group"
                        + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
                        + ".name&sysparm_limit=1&sysparm_offset=0>;rel=\"first\","
                        + "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_grmember?sysparm_query=group"
                        + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
                        + ".name&sysparm_limit=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group_users_batch_page1.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_grmember?sysparm_query=group"
            + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
            + ".name&sysparm_limit=1&sysparm_offset=1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_page2.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> groupUsers = (List<String>) entry.get("GroupUsers");
      assertThat(groupUsers).containsExactlyInAnyOrder("itil", "approver", "ATF.User", "admin");
    }
  }

  @Test
  @SneakyThrows
  void testGetPaginatedGroupsWithPaginatedGroupUsersWithSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_group?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_group?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_grmember?sysparm_query=group"
                        + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
                        + ".name&sysparm_limit=1&sysparm_offset=0>;rel=\"first\","
                        + "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_grmember?sysparm_query=group"
                        + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
                        + ".name&sysparm_limit=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group_users_batch_page1.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_grmember?sysparm_query=group"
            + ".name=MIF%20Admins&sysparm_fields=user.user_name,group"
            + ".name&sysparm_limit=1&sysparm_offset=1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_page2.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_users_batch_database.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalGroups = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> group : list) {
        totalGroups++;
        List<String> groupUsers = (List<String>) group.get("GroupUsers");
        String groupName = (String) group.get("Name");
        if ("MIF Admins".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName)
              .containsExactlyInAnyOrder("itil", "approver", "ATF.User", "admin");
        } else if ("Database".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName)
              .containsExactlyInAnyOrder("ATF.User");
        }
      }
    }
    assertEquals(2, totalGroups);
  }

  @Test
  @SneakyThrows
  void testGetGroupUsersWithInternalServerErrorWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withStatus(500)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupWithGroupRolesVerifyGroupRolesField() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_roles_batch_mif_admins.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> groupRoles = (List<String>) entry.get("GroupRoles");
      assertNotNull(groupRoles);
      assertEquals(2, groupRoles.size());
      assertEquals("app_engine_admin", groupRoles.get(0));
      assertEquals("activity_admin", groupRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithCaretInNameVerifyGroupRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=group^^test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("group_with_caret_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_roles_batch_group_with_caret.json"))));

    LdapSearchRequest groupWithCaretRequest = LdapSearchRequest.create(
        DN.from("group=group^test,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            groupWithCaretRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("group^test", entry.get("Name"));
      List<String> groupRoles = (List<String>) entry.get("GroupRoles");
      assertNotNull(groupRoles);
      assertEquals(2, groupRoles.size());
      assertEquals("role^admin", groupRoles.get(0));
      assertEquals("role,test", groupRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithCommaInNameVerifyGroupRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=group,test"))
            .willReturn(
                aResponse().withBody(TestUtil.getResponseFromFile("group_with_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group,test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group,test"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_roles_batch_group_with_comma.json"))));

    LdapSearchRequest groupWithCommaRequest = LdapSearchRequest.create(
        DN.from("group=group\\,test,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            groupWithCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("group,test", entry.get("Name"));
      List<String> groupRoles = (List<String>) entry.get("GroupRoles");
      assertNotNull(groupRoles);
      assertEquals(2, groupRoles.size());
      assertEquals("role^admin", groupRoles.get(0));
      assertEquals("role,test", groupRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithCaretAndCommaInNameVerifyGroupRolesField() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_query", WireMock.equalTo("name=group^^test,admin"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_with_caret_and_comma_test.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test,admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=group^^test,admin"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile(
                    "group_roles_batch_group_with_caret_and_comma.json"))));

    LdapSearchRequest groupWithCaretAndCommaRequest = LdapSearchRequest.create(
        DN.from("group=group^test\\,admin,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            groupWithCaretAndCommaRequest);
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      assertEquals("group^test,admin", entry.get("Name"));
      List<String> groupRoles = (List<String>) entry.get("GroupRoles");
      assertNotNull(groupRoles);
      assertEquals(2, groupRoles.size());
      assertEquals("role^admin", groupRoles.get(0));
      assertEquals("role,test", groupRoles.get(1));
    }
  }

  @Test
  @SneakyThrows
  void testGetGroupWithEmptyGroupRoles() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    int groupsFetched = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      groupsFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, groupsFetched);
  }

  @Test
  @SneakyThrows
  void testGetMultipleGroupsWithDifferentGroupRoles() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_group?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_group?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_roles_batch_mif_admins.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_roles_batch_database.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalGroups = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> group : list) {
        totalGroups++;
        List<String> groupRoles = (List<String>) group.get("GroupRoles");
        String groupName = (String) group.get("Name");
        assertNotNull(groupRoles, "GroupRoles should not be null for group: " + groupName);
        if ("MIF Admins".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName).hasSize(2);
        } else if ("Database".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName)
              .containsExactlyInAnyOrder("admin");
        }
      }
    }
    assertEquals(2, totalGroups);
  }

  @Test
  @SneakyThrows
  void testGetGroupGroupRolesWithInvalidNextLinkWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_group_has_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<invalid-url>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group_roles_batch_mif_admins.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetGroupGroupRolesWithNextLinkWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_group_has_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_group_has_role"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group_roles_batch_mif_admins.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_group_has_role?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("group_roles_empty.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      Map<String, Object> entry = list.get(0);
      List<String> groupRoles = (List<String>) entry.get("GroupRoles");
      assertNotNull(groupRoles);
      assertEquals(2, groupRoles.size());
    }
  }

  @Test
  @SneakyThrows
  void testGetPaginatedGroupsWithPaginatedGroupRolesWithSuccess() {
    wireMockServer.stubFor(
        get(urlEqualTo("/api/now/table/sys_user_group?sysparm_limit=1")).willReturn(
            aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_user_group"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_user_group?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("another_group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withHeader("Link",
                    "<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_group_has_role?sysparm_limit=1"
                        + "&sysparm_offset=0"
                        + ">;rel=\"first\",<http://localhost:" + wiremockPortNo
                        + "/api/now/table/sys_group_has_role"
                        + "?sysparm_limit"
                        + "=1&sysparm_offset=1>;rel=\"next\"")
                .withBody(TestUtil.getResponseFromFile("group_roles_batch_mif_admins.json"))));

    wireMockServer.stubFor(
        get(urlEqualTo(
            "/api/now/table/sys_group_has_role?sysparm_limit=1&sysparm_offset=1")).willReturn(
            aResponse().withBody(TestUtil.getResponseFromFile("group_roles_empty.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=Database"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("group_roles_batch_database.json"))));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalGroups = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> group : list) {
        totalGroups++;
        List<String> groupRoles = (List<String>) group.get("GroupRoles");
        String groupName = (String) group.get("Name");
        assertNotNull(groupRoles, "GroupRoles should not be null for group: " + groupName);
        if ("MIF Admins".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName).hasSize(2);
        } else if ("Database".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName).hasSize(1);
        }
      }
    }
    assertEquals(2, totalGroups);
  }

  @Test
  @SneakyThrows
  void testGetGroupGroupRolesWithInternalServerErrorWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_group")).willReturn(
        aResponse().withBody(TestUtil.getResponseFromFile("group.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo("group.name=MIF Admins"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("1"))
            .willReturn(aResponse().withStatus(500)));

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getSingleGroupLdapRequest());
    LdapResponse<String> response = dataProvider.next();
    assertEquals(OPERATIONS_ERROR, response.getStatus());
  }

  @Test
  @SneakyThrows
  void testGetMultipleGroupsInSinglePageWithGroupUsers() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("three_groups_single_page.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo(
                "group.name=IT Support^ORgroup.name=HR Team^ORgroup.name=Finance"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("three_groups_users.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo(
                "group.name=IT Support^ORgroup.name=HR Team^ORgroup.name=Finance"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    ReadOnlyProperties connectionPropertiesWithBatchSize3PageSize3 =
        TestUtil.getConnectionPropertiesWithBatchSize3AndPageSize3();
    HttpClient httpClient = new HttpClient(connectionPropertiesWithBatchSize3PageSize3);

    serviceNowConnector =
        new ServiceNowConnector(targetSchemaObjects, schema,
            connectionPropertiesWithBatchSize3PageSize3, httpClient);

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalGroups = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> group : list) {
        totalGroups++;
        List<String> groupUsers = (List<String>) group.get("GroupUsers");
        String groupName = (String) group.get("Name");
        if ("IT Support".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName)
              .containsExactlyInAnyOrder("alex");
        } else if ("HR Team".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName)
              .containsExactlyInAnyOrder("ariel");
        } else if ("Finance".equals(groupName)) {
          assertThat(groupUsers).as("GroupUsers for " + groupName)
              .containsExactlyInAnyOrder("bola");
        }
      }
    }
    assertEquals(3, totalGroups);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_group")));
    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
  }

  @Test
  @SneakyThrows
  void testGetMultipleGroupsInSinglePageWithGroupRolesBatchedRequest() {
    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_group"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("three_groups_single_page.json"))));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .withQueryParam("sysparm_query", WireMock.equalTo(
                "group.name=IT Support^ORgroup.name=HR Team^ORgroup.name=Finance"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("user.user_name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody("{\"result\":[]}")));

    wireMockServer.stubFor(
        get(urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query", WireMock.equalTo(
                "group.name=IT Support^ORgroup.name=HR Team^ORgroup.name=Finance"))
            .withQueryParam("sysparm_fields", WireMock.equalTo("role.name,group.name"))
            .withQueryParam("sysparm_limit", WireMock.equalTo("3"))
            .willReturn(aResponse().withBody(
                TestUtil.getResponseFromFile("three_groups_roles.json"))));

    ReadOnlyProperties connectionPropertiesWithBatchSize3PageSize3 =
        TestUtil.getConnectionPropertiesWithBatchSize3AndPageSize3();
    HttpClient httpClient = new HttpClient(connectionPropertiesWithBatchSize3PageSize3);

    serviceNowConnector =
        new ServiceNowConnector(targetSchemaObjects, schema,
            connectionPropertiesWithBatchSize3PageSize3, httpClient);

    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) serviceNowConnector.search(
            getAllGroupsLdapRequest());
    ObjectMapper objectMapper = new ObjectMapper();
    int totalGroups = 0;
    while (dataProvider.hasMore()) {
      String response = dataProvider.next().getData();
      List<Map<String, Object>> list =
          objectMapper.readValue(response,
              new TypeReference<List<Map<String, Object>>>() {
              });
      for (Map<String, Object> group : list) {
        totalGroups++;
        List<String> groupRoles = (List<String>) group.get("GroupRoles");
        String groupName = (String) group.get("Name");
        assertNotNull(groupRoles, "GroupRoles should not be null for group: " + groupName);
        if ("IT Support".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName)
              .containsExactlyInAnyOrder("app_engine_admin");
        } else if ("HR Team".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName)
              .containsExactlyInAnyOrder("user_admin");
        } else if ("Finance".equals(groupName)) {
          assertThat(groupRoles).as("GroupRoles for " + groupName)
              .containsExactlyInAnyOrder("admin");
        }
      }
    }
    assertEquals(3, totalGroups);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_group")));
    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_group_has_role"))
            .withQueryParam("sysparm_query",
                WireMock.equalTo(
                    "group.name=IT Support^ORgroup.name=HR Team^ORgroup.name=Finance")));
  }

  // ==================== Group-User Relationship Tests ====================

  @Test
  @SneakyThrows
  void testAddUsersToGroupWithSuccess() {
    stubGroupUserApiWithSuccess("addUsersIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1", "user2", "user3")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(3, "user1", "user2", "user3");
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInMultipleFormatInputWithSuccess() {
    stubGroupUserApiWithSuccess("addUsersMultiFormatIT", 3);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("[\"user1\"]", "user2", "user3")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(3, "user1", "user2", "user3");
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingCommaAsValueInJsonArrayWithIncorrectUserGotLinked() {
    stubGroupUserApiWithSuccess("addUsersCommaIT", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("[user1,]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(1, "user1");
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingCommaAsValueWithIncorrectUserGotLinked() {
    stubGroupUserApiWithSuccess("addUsersCommaIT2", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1,,user2")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(1, "user1,,user2");
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingCommaAsValueWithCorrectUserGotLinked() {
    stubGroupUserApiWithSuccess("addUsersCommaIT3", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1,")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(1, "user1,");
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingNullAsValueWithFailure() {

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyGroupUserApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingNullAsValueInJsonArrayWithNoUserToAdd() {

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("[null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupWithMixedValidUserAndNullInJsonArray() {
    stubGroupUserApiWithSuccess("testAddUsersToGroupWithMixedValidUserAndNullInJsonArray", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("[\"testuser\",null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(1, "testuser");
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingNullAsValueForReplaceModificationTypeWithFailure() {

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyGroupUserApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupInContainingNullAsValueInJsonArrayForReplaceWithNoUserToAdd() {

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers", Lists.newArrayList("[null]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(400)
            .withBody("{\"error\":{\"message\":\"Bad request\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUsersToGroupWithInternalServerError() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithReplaceModificationTypeWithJsonArrayInputWithSuccess() {
    stubCommonMocksForDeleteAllUsersFromGroup();
    stubGroupUserApiWithSuccess("replaceUsersJsonArrayIT", 2);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers",
            Lists.newArrayList("[\"user1\",\"user2\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(2, "user1", "user2");
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithReplaceModificationTypeWithSuccess() {
    stubCommonMocksForDeleteAllUsersFromGroup();
    stubGroupUserApiWithSuccess("replaceUsersSingleIT", 1);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers",
            Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(1, "user1");
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithReplaceModificationTypeWithBadRequest() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(400)
            .withBody("{\"error\":{\"message\":\"Bad request\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithReplaceModificationTypeWithInternalServerError() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithMultipleValuesWithReplaceModificationTypeWithSuccess() {
    stubCommonMocksForDeleteAllUsersFromGroup();
    stubGroupUserApiWithSuccess(
        "testAddGroupUsersWithMultipleValuesWithReplaceModificationTypeWithSuccess", 2);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("GroupUsers",
            Lists.newArrayList("[\"user1\",\"user2\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(2, "user1", "user2");
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithBlankValuesSkipsBlankAndAddsValidUsers() {
    stubGroupUserApiWithSuccess("testAddGroupUsersWithBlankValuesSkipsBlankAndAddsValidUsers", 1);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers",
            Lists.newArrayList("[\"user1\",\"\",\" \",\"  \"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    verifyGroupUserApiCalls(1, "user1");
  }

  @Test
  @SneakyThrows
  void testAddGroupUsersWithOnlyBlankValuesWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers",
            Lists.newArrayList("[\"\",\" \",\"  \"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    verifyGroupUserApiCalls(0);
  }

  @Test
  @SneakyThrows
  void testAddUserToGroupWith201ButBlankSysIdWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":{\"sys_id\":\"\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUserToGroupWith201ButNullResultWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{\"result\":null}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testAddUserToGroupWith201ButEmptyResponseWithFailure() {
    wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(201)
            .withBody("{}")));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithSuccess() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"user_name\":\"testuser\"},"
                    + "\"group\":{\"name\":\"test_group_name\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_grmember/membership123"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("testuser")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember/membership123")));
  }

  @Test
  @SneakyThrows
  void testDeleteMultipleUsersFromGroupWithSuccess() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_grmember?sysparm_query=user.user_name%3Dtestuser%5Egroup"
            + ".name%3Dtest_group_name&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"user_name\":\"testuser\"},"
                    + "\"group\":{\"name\":\"test_group_name\"}}]}")));

    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_grmember?sysparm_query=user.user_name%3Dtestuser1%5Egroup"
            + ".name%3Dtest_group_name&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership1234\","
                    + "\"user\":{\"user_name\":\"testuser\"},"
                    + "\"group\":{\"name\":\"test_group_name\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo(
        "/api/now/table/sys_user_grmember/membership123"))
        .willReturn(aResponse().withStatus(204)));

    wireMockServer.stubFor(delete(urlPathEqualTo(
        "/api/now/table/sys_user_grmember/membership1234"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification =
        Modification.create(Modification.ModificationType.DELETE,
            Attribute.create("GroupUsers", Lists.newArrayList("testuser", "testuser1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(2,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember/membership123")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember/membership1234")));
  }

  private void stubCommonMocksForDeleteAllUsersFromGroup() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_grmember?sysparm_query=group"
            + ".name%3Dtest_group_name&sysparm_fields=user.user_name%2Cgroup"
            + ".name&sysparm_limit=1&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"group.name\": \"test_group_name\",\n"
                    + "            \"user.user_name\": \"test_Create_User33\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")));

    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_grmember?sysparm_query=user"
            + ".user_name%3Dtest_Create_User33%5Egroup"
            + ".name%3Dtest_group_name&sysparm_fields=sys_id&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\n"
                    + "    \"result\": [\n"
                    + "        {\n"
                    + "            \"sys_id\": \"8550c832c3388b1019b4fa0ed4013181\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}")));

    wireMockServer.stubFor(delete(urlPathEqualTo(
        "/api/now/table/sys_user_grmember/8550c832c3388b1019b4fa0ed4013181"))
        .willReturn(aResponse().withStatus(204)));
  }

  @Test
  @SneakyThrows
  void testDeleteAllUsersFromGroupWithSuccess() {
    stubCommonMocksForDeleteAllUsersFromGroup();
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(2,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo(
                "/api/now/table/sys_user_grmember/8550c832c3388b1019b4fa0ed4013181")));
  }

  @Test
  @SneakyThrows
  void testDeleteAllUsersFromGroupWhenGroupHasNoUsersWithSuccess() {
    wireMockServer.stubFor(get(urlEqualTo(
        "/api/now/table/sys_user_grmember?sysparm_query=group"
            + ".name%3Dtest_group_name&sysparm_fields=user.user_name%2Cgroup"
            + ".name&sysparm_limit=1&inherited=false"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(
            WireMock.urlPathMatching("/api/now/table/sys_user_grmember/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithBlankValuesSkipsBlankAndDeletesValidUsers() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"user_name\":\"testuser\"},"
                    + "\"group\":{\"name\":\"test_group_name\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_grmember/membership123"))
        .willReturn(aResponse().withStatus(204)));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("testuser", "", " ", "  ")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember/membership123")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithOnlyBlankValuesWithSuccess() {
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("", " ", "  ")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(0,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_grmember/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWhenMembershipNotFoundWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("testuser")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_grmember/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWhenGetMembershipFails() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("testuser")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWhenDeleteFails() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody(
                "{\"result\":[{\"sys_id\":\"membership123\","
                    + "\"user\":{\"user_name\":\"testuser\"},"
                    + "\"group\":{\"name\":\"test_group_name\"}}]}")));

    wireMockServer.stubFor(delete(urlPathEqualTo("/api/now/table/sys_user_grmember/membership123"))
        .willReturn(aResponse().withStatus(500)
            .withBody("{\"error\":{\"message\":\"Internal server error\"}}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("testuser")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(1,
        WireMock.deleteRequestedFor(
            WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember/membership123")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithNullResponseBodyWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":null}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("testuser")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithJsonArrayStringWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("[\"testuser\"]")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_grmember/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithNullValueWithFailure() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/now/table/sys_user_grmember"))
        .willReturn(aResponse().withStatus(200)
            .withBody("{\"result\":[]}")));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("null")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);

    wireMockServer.verify(1,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_grmember/.*")));
  }

  @Test
  @SneakyThrows
  void testDeleteUserFromGroupWithBlankValueSkippedWithSuccess() {
    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("GroupUsers", Lists.newArrayList("")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(SUCCESS);

    wireMockServer.verify(0,
        WireMock.getRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));
    wireMockServer.verify(0,
        WireMock.deleteRequestedFor(WireMock.urlMatching("/api/now/table/sys_user_grmember/.*")));
  }

  @Test
  @SneakyThrows
  void testModifyGroupWithInvalidAttributeWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("InvalidAttribute", Lists.newArrayList("value1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testModifyGroupWithUnsupportedAttributeWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("GroupRoles", Lists.newArrayList("admin")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

    LdapResponse<?> actual = serviceNowConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testModifyGroupWithInvalidModificationTypeWithFailure() {
    Modification modification = Modification.create(Modification.ModificationType.INCREMENT,
        Attribute.create("GroupUsers", Lists.newArrayList("user1")));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification, GROUPS_DN);

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

  private void stubGroupUserApiWithSuccess(String scenarioName, int numberOfUsers) {
    for (int i = 0; i < numberOfUsers; i++) {
      String currentState = (i == 0) ? Scenario.STARTED : "CALL_" + i;
      String nextState = (i == numberOfUsers - 1) ? null : "CALL_" + (i + 1);
      String sysId = String.format("membership_%s_%03d", scenarioName, i);

      if (nextState != null) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(currentState)
            .willReturn(aResponse().withStatus(201)
                .withBody("{\"result\":{\"sys_id\":\"" + sysId + "\"}}"))
            .willSetStateTo(nextState));
      } else {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/now/table/sys_user_grmember"))
            .inScenario(scenarioName)
            .whenScenarioStateIs(currentState)
            .willReturn(aResponse().withStatus(201)
                .withBody("{\"result\":{\"sys_id\":\"" + sysId + "\"}}")));
      }
    }
  }

  private void verifyGroupUserApiCalls(int expectedCount, String... userNames) {
    wireMockServer.verify(expectedCount,
        WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember")));

    for (String userName : userNames) {
      wireMockServer.verify(1,
          WireMock.postRequestedFor(WireMock.urlPathEqualTo("/api/now/table/sys_user_grmember"))
              .withRequestBody(WireMock.containing("\"user\":\"" + userName + "\"")));
    }
  }

  private LdapSearchRequest getSingleGroupLdapRequest() {
    return LdapSearchRequest.create(
        DN.from("group=MIF Admins,ou=Groups,ou=ServiceNow"),
        SearchScope.BASE,
        SearchFilter.from("(objectclass=*)"));
  }

  private LdapSearchRequest getAllGroupsLdapRequest() {
    return LdapSearchRequest.create(DN.from("ou=Groups"), SearchScope.SUB,
        SearchFilter.from("(objectclass=*)"));
  }
}
