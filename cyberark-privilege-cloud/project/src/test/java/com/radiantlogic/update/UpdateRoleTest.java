package com.radiantlogic.update;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Groups;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.Roles;
import com.radiantlogic.utility.Users;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.UPDATE_ROLE_ENDPOINT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class UpdateRoleTest {

  RequestBuilder requestBuilder;

  RequestExecutor executor;
  GetRequest getRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  UpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    getRequest = mock(GetRequest.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    updateRequest =
        new UpdateRequest(requestBuilder, getRequest, executor, targetSchemaObjects, schema);
    when(executor.canConnect()).thenReturn(true);
    when(executor.getTokenWithBearerPrefixed()).thenReturn("Bearer ey..");
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("role"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForRole()));
  }

  @Test
  @SneakyThrows
  void addRoleDescription() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");
    roleRecordRow.setDescription("new role description");

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-description", "new role description"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateRoleDescription() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");
    roleRecordRow.setDescription("updated role desc");

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-description", "updated role desc"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleDescription() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");
    roleRecordRow.setDescription("");

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-description", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteAllRoleMembers() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> roleMembers =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_role_members.json"),
            CyberArkResponse.class).getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Users userMembersToBeDeleted = new Users();
    userMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    roleRecordRow.setUsers(userMembersToBeDeleted);

    Roles roleMembersToBeDeleted = new Roles();
    roleMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("1b80ce35_1234_4503_a32f_c0fd5c5f9974");
    roleRecordRow.setRoles(roleMembersToBeDeleted);

    when(getRequest.getRoleMembers(any())).thenReturn(roleMembers);
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleMembersOfUserType() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Users userMembersToBeDeleted = new Users();
    userMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    roleRecordRow.setUsers(userMembersToBeDeleted);

    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members",
            Lists.newArrayList("(User)abcc1s232@cyberark.cloud.39470")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleMembersOfRoleType() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> anotherRole =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_role.json"),
            CyberArkResponse.class).getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Roles roleMembersToBeDeleted = new Roles();
    roleMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("be3c524a_1234_489b_b10a_69a9ebbfbc58");
    roleRecordRow.setRoles(roleMembersToBeDeleted);

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(getRequest.getRoles(0, "test role", false, null, null, false, null)).thenReturn(
        anotherRole);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateRoleMember() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> anotherRole =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_role.json"),
            CyberArkResponse.class).getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Roles roleMembersToBeAdded = new Roles();
    roleMembersToBeAdded.getKeysToAddAsRoleMembers().add("be3c524a_1234_489b_b10a_69a9ebbfbc58");
    roleRecordRow.setRoles(roleMembersToBeAdded);

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles, anotherRole);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members",
            Lists.newArrayList("(Role)test role", "be3c524a_1234_489b_b10a_69a9ebbfbc58")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }


  @Test
  @SneakyThrows
  void addRoleMembers() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> anotherUser =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_user.json"),
            CyberArkResponse.class).getResultData().getRecords();
    List<Record> anotherRole =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("another_role.json"),
            CyberArkResponse.class).getResultData().getRecords();

    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Users userMembers = new Users();
    userMembers.getKeysToAddAsRoleMembers().add("c054b40b-9682-45bd-b186-a256f737744f");
    roleRecordRow.setUsers(userMembers);

    Roles roleMembers = new Roles();
    roleMembers.getKeysToAddAsRoleMembers().add("be3c524a_1234_489b_b10a_69a9ebbfbc58");
    roleRecordRow.setRoles(roleMembers);

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles, anotherRole);
    when(getRequest.getUsers(anyInt(), any(), any(), any(), any())).thenReturn(anotherUser);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-members", Lists.newArrayList("c054b40b-9682-45bd-b186-a256f737744f",
            "[be3c524a_1234_489b_b10a_69a9ebbfbc58]")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
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

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteInvalidUserMember() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members",
            Lists.newArrayList("(User)abcc1s232@cyberark.cloud.39470")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteInvalidRoleMember() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(getRequest.getRoles(0, "test role", false, null, null, false, null)).thenReturn(
        new ArrayList<>());

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteInvalidRoleMemberType() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Invalid_type)test role")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void addRoleMemberWithSameIdAsRoleId() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-members",
            Lists.newArrayList("be3c524a_1e5e_489b_b10a_69a9ebbfbc58")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleMemberWithSameIdAsRoleId() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members",
            Lists.newArrayList("be3c524a_1e5e_489b_b10a_69a9ebbfbc58")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleMemberWithEmptyList() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members", new ArrayList<>()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleMemberWithEmptyMemberId() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role", " ")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateRoleWithInvalidModificationType() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), any(), anyBoolean(),
        any())).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.INCREMENT,
        Attribute.create("role-members", Lists.newArrayList("(Role)test role", " ")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }


  @Test
  @SneakyThrows
  void addNotAllowedRoleAttribute() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.ADD,
        Attribute.create("role-id", "new role id"));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteNotAllowedRoleAttribute() {
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-id", new ArrayList<>()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  RequestBody getUpdateRolePayload(final RecordRow userRecordRow) {
    String jsonString = GOOGLE_JSON.toJson(userRecordRow);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  @Test
  @SneakyThrows
  void deleteRoleMembersOfGroupType() {
    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Groups roleMembersToBeDeleted = new Groups();
    roleMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("ExampleOrganization");
    roleRecordRow.setGroups(roleMembersToBeDeleted);

    setCommonUpdateRoleMocks();
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList("(Group)ExampleOrganization")));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteAllRoleMembersContainingGroupMember() {
    List<Record> roleMembers =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role_members_with_group_members.json"),
            CyberArkResponse.class).getResultData().getRecords();

    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    Users userMembersToBeDeleted = new Users();
    userMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    roleRecordRow.setUsers(userMembersToBeDeleted);

    Roles roleMembersToBeDeleted = new Roles();
    roleMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("1b80ce35_1234_4503_a32f_c0fd5c5f9974");
    roleRecordRow.setRoles(roleMembersToBeDeleted);

    Groups groupMembersToBeDeleted = new Groups();
    groupMembersToBeDeleted.getKeysToDeleteAsRoleMembers()
        .add("ExampleOrganization");
    roleRecordRow.setGroups(groupMembersToBeDeleted);

    setCommonUpdateRoleMocks();
    when(getRequest.getRoleMembers(any())).thenReturn(roleMembers);
    when(requestBuilder.getUpdateRolePayload(any())).thenReturn(
        getUpdateRolePayload(roleRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("role-members", Lists.newArrayList()));

    final LdapModifyRequest ldapModifyRequest =
        LdapModifyRequest.builder().dn(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"))
            .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  void setCommonUpdateRoleMocks() throws CyberArkPrivilegeCloudException, IOException {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    ResponseData updateRoleResponse = new ResponseData();
    updateRoleResponse.setResponseCode(200);
    updateRoleResponse.setApiResponse(TestUtils.getOperationSuccessResponse());
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(requestBuilder.getUpdateRoleUrl()).thenReturn(httpUrl);
    when(executor.execute(any())).thenReturn(updateRoleResponse);
  }

}
