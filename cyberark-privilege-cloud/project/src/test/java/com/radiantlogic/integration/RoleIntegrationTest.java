package com.radiantlogic.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.radiantlogic.utility.Constants.CREATE_ROLE_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_ROLES_ENDPOINT;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.GET_ROLE_MEMBERS_ENDPOINT;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static com.radiantlogic.utility.Constants.RED_ROCK_ENDPOINT;
import static com.radiantlogic.utility.Constants.ROLE_RIGHTS_ENDPOINT;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.UPDATE_ROLE_ENDPOINT;
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
import java.util.ArrayList;
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
class RoleIntegrationTest {

  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  CyberArkConnector cyberArkConnector;
  ReadOnlyProperties connectionProperties;
  private WireMockServer wireMockServer;

  static final String ROLE_NAME_KEY = "role-name";
  static final String ROLE_DESCRIPTION_KEY = "role-description";
  static final String ROLE_TYPE_KEY = "role-type";
  static final String ROLE_MEMBERS_KEY = "role-members";

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
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("role"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForRole()));
    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));
  }

  @Test
  @SneakyThrows
  void createRole() {
    wireMockServer.stubFor(post(urlEqualTo("/" + CREATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("create_role.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("verifyusersandmembersforcreaterole")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("user.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("verifyusersandmembersforcreaterole")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = cyberArkConnector.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateRoleDescription() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-description", "updated role desc"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void addRoleDescription() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-description", "new role description"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleDescription() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-description", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteAllRoleMembers() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    wireMockServer.stubFor(post(urlEqualTo("/" + GET_ROLE_MEMBERS_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("another_role_members.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleMembersOfUserType() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("user.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members",
            Lists.newArrayList("(User)abcc1s232@cyberark.cloud.39470")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleMembersOfRoleType() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateRoleMember() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("updaterolememberIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("role.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("updaterolememberIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("updaterolememberIT")
            .whenScenarioStateIs("THIRD_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members",
            Lists.newArrayList("(Role)test role", "be3c524a_1234_489b_b10a_69a9ebbfbc58")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void addRoleMembers() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("addRoleMembersIT")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("role.json")))
        .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("addRoleMembersIT")
        .whenScenarioStateIs("SECOND_CALL_STATE")
        .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_user.json")))
        .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("addRoleMembersIT")
        .whenScenarioStateIs("THIRD_CALL_STATE")
        .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-members", Lists.newArrayList("c054b40b-9682-45bd-b186-a256f737744f",
            "[be3c524a_1234_489b_b10a_69a9ebbfbc58]")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateInvalidRole() {
    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-description", "new role description"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteInvalidUserMember() {
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("deleteInvalidUserMemberIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("role.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("deleteInvalidUserMemberIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_users.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members",
            Lists.newArrayList("(User)abcc1s232@cyberark.cloud.39470")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteInvalidRoleMember() {
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("deleteInvalidRoleMemberIT")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("role.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("deleteInvalidRoleMemberIT")
            .whenScenarioStateIs("SECOND_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteInvalidRoleMemberType() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Invalid_type)test role")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void addRoleMemberWithSameIdAsRoleId() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-members",
            Lists.newArrayList("be3c524a_1e5e_489b_b10a_69a9ebbfbc58")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleMemberWithSameIdAsRoleId() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members",
            Lists.newArrayList("be3c524a_1e5e_489b_b10a_69a9ebbfbc58")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleMemberWithEmptyList() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members", new ArrayList<>()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleMemberWithEmptyMemberId() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role", " ")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleWithInvalidModificationType() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.INCREMENT,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role", " ")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void addNotAllowedRoleAttribute() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-id", "new role id"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteNotAllowedRoleAttribute() {
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-id", new ArrayList<>()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteRole() {
    wireMockServer.stubFor(post(urlEqualTo("/" + DELETE_ROLES_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));

    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = cyberArkConnector.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  void getPaginatedRoles() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();

    wireMockServer.stubFor(post(urlEqualTo("/" + GENERATE_ACCESS_TOKEN_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("access_token.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedrolesIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedrolesIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedrolesIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedrolesIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("roles.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedrolesIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json")))
            .willSetStateTo("SIXTH_CALL_STATE"));


    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario("getpaginatedrolesrightsIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("role_rights.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario("getpaginatedrolesrightsIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario("getpaginatedrolesrightsIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getpaginatedrolesIT")
            .whenScenarioStateIs("SIXTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from("(objectclass=*)"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(3, totalRolesFetched);
  }

  @Test
  void getRoleByRoleName() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getrolebynameIT")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("role.json")))
        .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getrolebynameIT")
        .whenScenarioStateIs("SECOND_CALL_STATE")
        .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(post(urlEqualTo("/" + GET_ROLE_MEMBERS_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role_members.json"))));

    wireMockServer.stubFor(post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"),
            SearchScope.BASE, SearchFilter.from("(objectclass=*)"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalRolesFetched);
  }

  @Test
  @SneakyThrows
  void deleteRoleMemberOfGroupType() {
    setCommonWireMocksForUpdateRole();

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members",
            Lists.newArrayList("(Group)HR")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteAllRoleMembersContainingGroupMember() {
    setCommonWireMocksForUpdateRole();

    wireMockServer.stubFor(post(urlEqualTo("/" + GET_ROLE_MEMBERS_ENDPOINT)).willReturn(
        aResponse().withBody(
            TestUtils.getResponseFromFile("role_members_with_group_members.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  private void setCommonWireMocksForUpdateRole() {
    wireMockServer.stubFor(post(urlEqualTo("/" + UPDATE_ROLE_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getOperationSuccessResponse())));

    wireMockServer.stubFor(post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).willReturn(
        aResponse().withBody(TestUtils.getResponseFromFile("role.json"))));
  }

  @Test
  @SneakyThrows
  void deleteAllRoleMembersContainingInvalidMemberType() {
    setCommonWireMocksForUpdateRole();

    wireMockServer.stubFor(post(urlEqualTo("/" + GET_ROLE_MEMBERS_ENDPOINT)).willReturn(
        aResponse().withBody(
            TestUtils.getResponseFromFile("role_members_with_invalid_member_type.json"))));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = cyberArkConnector.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  void getRoleByNameWithFilters() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRoleByNameWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdRole)(name=test role))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalRolesFetched);
  }

  @Test
  void getRolesByNameWithFiltersWithMultipleValues() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByNameWithFiltersIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRoleByNameWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from("(&(name=test role1)(name=test role))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalRolesFetched);
  }

  @Test
  void getRolesByRoleTypeWithFilters() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("roles.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdRole)(roletype=PRINCIPAL LIST))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalRolesFetched);
  }

  @Test
  void getRoleByIdWithFilters() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByIdWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByIdWithFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByIdWithFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByIdWithFiltersIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByIdWithFiltersIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRoleByIdWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=vdRole)(id=be3c524a_1234_489b_b10a_69a9ebbfbc58))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalRolesFetched);
  }

  @Test
  void getRoleByDescriptionWithFilters() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByDescriptionWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByDescriptionWithFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByDescriptionWithFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByDescriptionWithFiltersIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRoleByDescriptionWithFiltersIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRoleByDescriptionWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from(
                "(&(objectclass=vdRole)(description=This role gives advanced monitoring rights to" +
                    " TDR Space.))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalRolesFetched);
  }

  @Test
  void getRolesByProvidingPartialDescriptionWithFilters() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getRolesByProvidingPartialDescriptionWithFilters")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getRolesByProvidingPartialDescriptionWithFilters")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getRolesByProvidingPartialDescriptionWithFilters")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getRolesByProvidingPartialDescriptionWithFilters")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("another_role.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario(
                "getRolesByProvidingPartialDescriptionWithFilters")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRolesByProvidingPartialDescriptionWithFilters")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from(
                "(&(objectclass=vdRole)(description=*advanced monitoring*))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(1, totalRolesFetched);
  }

  @Test
  void getRolesByRoleTypeWithPresenceFilter() throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("roles_with_members.json")))
            .willSetStateTo("SECOND_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("SECOND_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("another_roles_with_members" +
                    ".json")))
            .willSetStateTo("THIRD_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("THIRD_CALL_STATE").willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_roles_with_members" +
                    ".json")))
            .willSetStateTo("FOURTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("FOURTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("roles.json")))
            .willSetStateTo("FIFTH_CALL_STATE"));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + RED_ROCK_ENDPOINT)).inScenario("getRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs("FIFTH_CALL_STATE")
            .willReturn(aResponse().withBody(TestUtils.getResponseFromFile("empty_roles.json"))));

    wireMockServer.stubFor(
        post(urlEqualTo("/" + ROLE_RIGHTS_ENDPOINT)).inScenario(
                "roleRightsForGetRolesByRoleTypeWithFiltersIT")
            .whenScenarioStateIs(Scenario.STARTED).willReturn(
                aResponse().withBody(TestUtils.getResponseFromFile("empty_role_rights.json"))));

    final LdapSearchRequest ldapSearchRequest =
        LdapSearchRequest.create(DN.from("ou=Roles"), SearchScope.SUB,
            SearchFilter.from("(&(objectclass=*)(roletype=*))"));
    SequentialDataProvider<LdapResponse<String>> dataProvider =
        (SequentialDataProvider<LdapResponse<String>>) cyberArkConnector.search(ldapSearchRequest);
    int totalRolesFetched = 0;
    while (dataProvider.hasMore()) {
      totalRolesFetched += objectMapper.readTree(dataProvider.next().getData()).size();
    }
    assertEquals(2, totalRolesFetched);
  }
}