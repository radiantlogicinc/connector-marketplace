package com.radiantlogic.misc;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.radiantlogic.enums.RoleType;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.RolePojo;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class RequestBuilderTest {

  RequestBuilder requestBuilder;
  ReadOnlyProperties connectionProperties;

  @BeforeEach
  void setUp() {
    connectionProperties = TestUtils.getConnectionProperties();
    requestBuilder = new RequestBuilder(connectionProperties);
  }

  @Test
  @SneakyThrows
  void fetchTokenRequest() {
    assertNotNull(requestBuilder.getTokenRequest(), "");
  }

  @Test
  @SneakyThrows
  void fetchRedRockUrl() {
    assertEquals(String.format("http://localhost:%s/RedRock/query", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getRedRockUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesPayloadWithPageNumber() {
    assertNotNull(requestBuilder.getRolesPayload(1, "", "", null, null), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesPayloadWithRoleName() {
    assertNotNull(requestBuilder.getRolesPayload(0, "role 30", "", null, null), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesPayloadWithRoleID() {
    assertNotNull(
        requestBuilder.getRolesPayload(0, "", "be3c524a_1e5e_489b_b10a_69a9ebbfbc58", null, null),
        "");
  }

  @Test
  @SneakyThrows
  void fetchRolesPayloadWithRoleIDs() {
    Set<String> ids = new HashSet<>(Arrays.asList("be3c524a_1e5e_489b_b10a_69a9ebbfbc58",
        "be3c524a_bbbd_489b_b10a_69a9ebbfbc58"));
    assertNotNull(requestBuilder.getRolesPayload(0, "", "", ids, null), "");
  }

  @Test
  @SneakyThrows
  void fetchRoleMembersUrl() {
    assertEquals(String.format("http://localhost:%s/Roles/GetRoleMembers", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getRoleMembersUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesMembersPayload() {
    assertNotNull(requestBuilder.getRoleMembersPayload("be3c524a_1e5e_489b_b10a_69a9ebbfbc58"), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesWithMembersPayload() {
    assertNotNull(requestBuilder.getRolesWithMembersListPayload(1), "");
  }

  @Test
  @SneakyThrows
  void fetchUserPayloadWithPageNumber() {
    assertNotNull(requestBuilder.getUsersPayload(1, "", "", null, null), "");
  }

  @Test
  @SneakyThrows
  void fetchUserPayloadWithUsername() {
    assertNotNull(
        requestBuilder.getUsersPayload(0, "abcc1s232@cyberark.cloud.39470", "", null, null),
        "");
  }

  @Test
  @SneakyThrows
  void fetchUserPayloadWithId() {
    assertNotNull(
        requestBuilder.getUsersPayload(0, "", "8d64055b-1234-4550-9dbb-5c8f2bce35db", null, null),
        "");
  }

  @Test
  @SneakyThrows
  void fetchUsersPayloadWithUserIDs() {
    Set<String> ids = new HashSet<>(Arrays.asList("be3c524a_1e5e_489b_b10a_69a9ebbfbc58",
        "be3c524a_bbbd_489b_b10a_69a9ebbfbc58"));
    assertNotNull(requestBuilder.getUsersPayload(0, "", "", ids, null), "");
  }

  @Test
  @SneakyThrows
  void fetchDeleteRoleUrl() {
    assertEquals(
        String.format("http://localhost:%s/saasManage/DeleteRoles", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getDeleteRoleUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchDeleteRolePayload() {
    assertNotNull(requestBuilder.getDeleteRolePayload("8d64055b-1234-4550-9dbb-5c8f2bce35db"), "");
  }

  @Test
  @SneakyThrows
  void fetchDeleteUserUrl() {
    assertEquals(String.format("http://localhost:%s/UserMgmt/RemoveUsers", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getDeleteUserUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchDeleteUserPayload() {
    assertNotNull(requestBuilder.getDeleteUserPayload("8d64055b-1234-4550-9dbb-5c8f2bce35db"), "");
  }

  @Test
  @SneakyThrows
  void fetchUpdateUserUrl() {
    assertEquals(String.format("http://localhost:%s/User/UpdateProfile", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getUpdateUserUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchUpdateUserPayload() {
    RecordRow recordRow = new RecordRow();
    recordRow.setId("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    recordRow.setDisplayName("new display name");
    assertNotNull(requestBuilder.getUpdateUserPayload(recordRow), "");
  }

  @Test
  @SneakyThrows
  void fetchUpdateRoleUrl() {
    assertEquals(String.format("http://localhost:%s/Roles/UpdateRole", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getUpdateRoleUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchUpdateRolePayload() {
    RecordRow recordRow = new RecordRow();
    recordRow.setName("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    recordRow.setDescription("new description");
    assertNotNull(requestBuilder.getUpdateRolePayload(recordRow), "");
  }

  @Test
  @SneakyThrows
  void fetchCreateRoleUrl() {
    assertEquals(String.format("http://localhost:%s/Roles/StoreRole", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getCreateRoleUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchCreateRolePayload() {
    RolePojo rolePojo = new RolePojo();
    rolePojo.setName("role name");
    rolePojo.setDescription("role desc");
    rolePojo.setRoleType(RoleType.PRINCIPAL_LIST.getAttributeName());
    rolePojo.setUsers(Collections.singleton("8d64055b-1234-4550-9dbb-5c8f2bce35db"));
    rolePojo.setRoles(Collections.singleton("8d64055b-5555-4550-9dbb-5c8f2bce35db"));
    assertNotNull(requestBuilder.getCreateRolePayload(rolePojo), "");
  }

  @Test
  @SneakyThrows
  void fetchCreateUserUrl() {
    assertEquals(
        String.format("http://localhost:%s/CDirectoryService/CreateUser", PORT_NUMBER_FOR_TESTING),
        requestBuilder.getCreateUserUrl().toString(), "");
  }

  @Test
  @SneakyThrows
  void fetchCreateUserPayload() {
    RecordRow recordRow = new RecordRow();
    recordRow.setName("abcc1s232@cyberark.cloud.39470");
    recordRow.setMail("a@g.com");
    recordRow.setDisplayName("display name");
    recordRow.setMobile("+91112233");
    assertNotNull(requestBuilder.getCreateUserPayload(recordRow), "");
  }
}
