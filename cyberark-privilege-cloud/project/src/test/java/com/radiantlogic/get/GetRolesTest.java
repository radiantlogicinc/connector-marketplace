package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.GET_ROLES_BY_IDS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLES_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLES_WITH_MEMBERS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLE_BY_ID_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLE_BY_NAME_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLE_MEMBERS_ENDPOINT;
import static com.radiantlogic.utility.Constants.GET_ROLE_MEMBERS_PAYLOAD;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.RED_ROCK_ENDPOINT;
import static com.radiantlogic.utility.Constants.ROLE_RIGHTS_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class GetRolesTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  GetRequest getRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    getRequest = new GetRequest(requestBuilder, executor);
    when(executor.canConnect()).thenReturn(true);
    when(executor.getTokenWithBearerPrefixed()).thenReturn("Bearer ey..");
  }

  @Test
  @SneakyThrows
  void fetchRolesForParticularPageWithRoleRights() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();
    HttpUrl roleRightsUrl = TestUtils.getBaseUrl().addPathSegments(ROLE_RIGHTS_ENDPOINT).build();

    ResponseData responseData = TestUtils.getRolesResponse();
    ResponseData rolesRightsResponse = TestUtils.getRoleRightsResponse();
    ResponseData emptyRolesRightsResponse = TestUtils.getEmptyRoleRightsResponse();

    RequestBody requestBody = getRolesPayload(1, null, null, new HashSet<>());
    RequestBody roleRightsPayloadForAdvancedTDRMonitoringRole =
        TestUtils.getRoleRightsPayload("be3c524a_1234_1234_1234_69a9ebbfbc58");
    RequestBody roleRightsPayloadForCloudOnboardingAdmin =
        TestUtils.getRoleRightsPayload("1b80ce35_1235_4503_a32f_c0fd5c5f9974");

    when(requestBuilder.getPageSize()).thenReturn(100);
    when(requestBuilder.getRoleRightsUrl()).thenReturn(roleRightsUrl);
    when(requestBuilder.getRoleRightsPayload(any())).thenReturn(
        roleRightsPayloadForAdvancedTDRMonitoringRole, roleRightsPayloadForCloudOnboardingAdmin);
    when(executor.execute(any())).thenReturn(responseData, rolesRightsResponse,
        emptyRolesRightsResponse);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> roles =
        getRequest.getRoles(1, null, false, null, new HashSet<>(), true, new HashMap<>());
    assertEquals(2, roles.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getRoles(1, null, false, null, new HashSet<>(), false, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void fetchRolesForParticularPageWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody = getRolesPayload(1, null, null, new HashSet<>());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getRoles(1, null, false, null, new HashSet<>(), false, new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void fetchRoleByNameWithMembersAndRoleRights() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();
    HttpUrl roleRightsUrl = TestUtils.getBaseUrl().addPathSegments(ROLE_RIGHTS_ENDPOINT).build();

    ResponseData rolesResponse = TestUtils.getAnotherRoleResponse();
    ResponseData rolesRightsResponse = TestUtils.getRoleRightsResponse();

    ResponseData roleMembersResponse = new ResponseData();
    roleMembersResponse.setResponseCode(200);
    roleMembersResponse.setApiResponse(TestUtils.getResponseFromFile("role_members.json"));

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody = getRolesPayload(0, "test role", null, new HashSet<>());
    RequestBody requestBody1 = getRoleMembersPayload("be3c524a_1234_489b_b10a_69a9ebbfbc58");
    RequestBody roleRightsPayload =
        TestUtils.getRoleRightsPayload("be3c524a_1234_489b_b10a_69a9ebbfbc58");

    when(executor.execute(any())).thenReturn(rolesResponse, roleMembersResponse,
        rolesRightsResponse);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRoleMembersUrl()).thenReturn(
        TestUtils.getBaseUrl().addPathSegments(GET_ROLE_MEMBERS_ENDPOINT).build());
    when(requestBuilder.getRoleRightsUrl()).thenReturn(roleRightsUrl);
    when(requestBuilder.getRoleMembersPayload(any())).thenReturn(requestBody1);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);
    when(requestBuilder.getRoleRightsPayload(any())).thenReturn(roleRightsPayload);

    List<Record> roles =
        getRequest.getRoles(0, "test role", true, null, new HashSet<>(), true, new HashMap<>());
    assertEquals(1, roles.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchRoleById() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = TestUtils.getAnotherRoleResponse();

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody =
        getRolesPayload(0, null, "be3c524a_1234_489b_b10a_69a9ebbfbc58", new HashSet<>());

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> roles = getRequest.getRoles(0, null, false, "be3c524a_1234_489b_b10a_69a9ebbfbc58",
        new HashSet<>(), false, new HashMap<>());
    assertEquals(1, roles.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesByIds() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = TestUtils.getRolesResponse();

    Set<String> roleIds = new HashSet<>(Arrays.asList("be3c524a_1234_1234_1234_69a9ebbfbc58",
        "1b80ce35_1235_4503_a32f_c0fd5c5f9974"));
    RequestBody requestBody = getRolesPayload(0, null, null, roleIds);

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        requestBody);

    List<Record> roles = getRequest.getRoles(0, null, false, null, roleIds, false, new HashMap<>());
    assertEquals(2, roles.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchRoleMembersWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getRoleMembers("be3c524a_1234_489b_b10a_69a9ebbfbc58"));
  }

  @Test
  @SneakyThrows
  void fetchRoleMembersWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(GET_ROLE_MEMBERS_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    RequestBody requestBody = getRoleMembersPayload("be3c524a_1234_489b_b10a_69a9ebbfbc58");

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRoleMembersUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRoleMembersPayload(any())).thenReturn(requestBody);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getRoleMembers("be3c524a_1234_489b_b10a_69a9ebbfbc58"));
  }

  @Test
  @SneakyThrows
  void fetchRolesWithMembers() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData rolesWithMembersResponse = TestUtils.getRolesWithMembersResponse();

    ResponseData emptyRolesResponse = TestUtils.getEmptyRolesResponse();
    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody1 = getRolesWithMembersListPayload(1);
    RequestBody requestBody2 = getRolesWithMembersListPayload(2);

    when(executor.execute(any())).thenReturn(rolesWithMembersResponse, emptyRolesResponse);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesWithMembersListPayload(anyInt())).thenReturn(requestBody1,
        requestBody2);

    List<Record> rolesWithMembers = getRequest.getRolesWithMembersList();
    assertEquals(2, rolesWithMembers.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesWithMembersWithTokenExpired() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData rolesWithMembersResponse = TestUtils.getRolesWithMembersResponse();

    ResponseData tokenExpiredResponse = new ResponseData();
    tokenExpiredResponse.setResponseCode(401);

    ResponseData emptyRolesResponse = new ResponseData();
    emptyRolesResponse.setResponseCode(200);
    emptyRolesResponse.setApiResponse(
        TestUtils.getResponseFromFile("empty_roles_with_members.json"));

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody1 = getRolesWithMembersListPayload(1);
    RequestBody requestBody2 = getRolesWithMembersListPayload(2);

    when(executor.execute(any())).thenReturn(rolesWithMembersResponse, tokenExpiredResponse,
        emptyRolesResponse);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesWithMembersListPayload(anyInt())).thenReturn(requestBody1,
        requestBody2);
    doNothing().when(executor).generateToken();
    List<Record> rolesWithMembers = getRequest.getRolesWithMembersList();
    assertEquals(2, rolesWithMembers.size(), "");
  }

  @Test
  @SneakyThrows
  void fetchRolesWithMembersWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody requestBody = getRolesWithMembersListPayload(1);

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRolesWithMembersListPayload(anyInt())).thenReturn(requestBody);

    assertThrows(IllegalStateException.class, () -> getRequest.getRolesWithMembersList());
  }

  @Test
  @SneakyThrows
  void fetchRolesWithMembersWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> getRequest.getRolesWithMembersList());
  }

  RequestBody getRolesWithMembersListPayload(final int pageNumber) {
    String jsonString =
        String.format(GET_ROLES_WITH_MEMBERS_PAYLOAD, pageNumber, requestBuilder.getPageSize(),
            requestBuilder.getPageSize());
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  RequestBody getRoleMembersPayload(final String roleId) {
    String jsonString = String.format(GET_ROLE_MEMBERS_PAYLOAD, roleId);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  RequestBody getRolesPayload(final int pageNumber, final String roleName, final String roleId,
                              final Set<String> roleIds) {
    String jsonString;
    if (org.apache.commons.lang3.StringUtils.isNotBlank(roleName)) {
      jsonString = String.format(GET_ROLE_BY_NAME_PAYLOAD, roleName);
    } else if (org.apache.commons.lang3.StringUtils.isNotBlank(roleId)) {
      jsonString = String.format(GET_ROLE_BY_ID_PAYLOAD, roleId);
    } else if (roleIds != null && !roleIds.isEmpty()) {
      jsonString = String.format(GET_ROLES_BY_IDS_PAYLOAD,
          roleIds.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
    } else {
      jsonString = String.format(GET_ROLES_PAYLOAD, pageNumber, requestBuilder.getPageSize(),
          requestBuilder.getPageSize());
    }
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  @Test
  @SneakyThrows
  void getRoleByNameWithInvalidRoleRightsResponse() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();
    HttpUrl roleRightsUrl = TestUtils.getBaseUrl().addPathSegments(ROLE_RIGHTS_ENDPOINT).build();

    ResponseData rolesResponse = TestUtils.getAnotherRoleResponse();
    ResponseData rolesRightsResponse = TestUtils.getRoleRightsWithInvalidResponse();

    ResponseData roleMembersResponse = new ResponseData();
    roleMembersResponse.setResponseCode(200);
    roleMembersResponse.setApiResponse(TestUtils.getResponseFromFile("role_members.json"));

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody rolesPayload = getRolesPayload(0, "test role", null, new HashSet<>());
    RequestBody roleMembersPayload = getRoleMembersPayload("be3c524a_1234_489b_b10a_69a9ebbfbc58");
    RequestBody roleRightsPayload =
        TestUtils.getRoleRightsPayload("be3c524a_1234_489b_b10a_69a9ebbfbc58");

    when(executor.execute(any())).thenReturn(rolesResponse, roleMembersResponse,
        rolesRightsResponse);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRoleMembersUrl()).thenReturn(
        TestUtils.getBaseUrl().addPathSegments(GET_ROLE_MEMBERS_ENDPOINT).build());
    when(requestBuilder.getRoleRightsUrl()).thenReturn(roleRightsUrl);
    when(requestBuilder.getRoleMembersPayload(any())).thenReturn(roleMembersPayload);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        rolesPayload);
    when(requestBuilder.getRoleRightsPayload(any())).thenReturn(roleRightsPayload);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getRoles(0, "test role", true, null, new HashSet<>(), true,
            new HashMap<>()));
  }

  @Test
  @SneakyThrows
  void getRoleRightsWithInvalidCredentials() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();
    HttpUrl roleRightsUrl = TestUtils.getBaseUrl().addPathSegments(ROLE_RIGHTS_ENDPOINT).build();

    ResponseData rolesResponse = TestUtils.getAnotherRoleResponse();

    ResponseData roleMembersResponse = new ResponseData();
    roleMembersResponse.setResponseCode(200);
    roleMembersResponse.setApiResponse(TestUtils.getResponseFromFile("role_members.json"));

    when(requestBuilder.getPageSize()).thenReturn(100);
    RequestBody rolesPayload = getRolesPayload(0, "test role", null, new HashSet<>());
    RequestBody roleMembersPayload = getRoleMembersPayload("be3c524a_1234_489b_b10a_69a9ebbfbc58");

    when(executor.execute(any())).thenReturn(rolesResponse, roleMembersResponse);
    when(requestBuilder.getRedRockUrl()).thenReturn(httpUrl);
    when(requestBuilder.getRoleMembersUrl()).thenReturn(
        TestUtils.getBaseUrl().addPathSegments(GET_ROLE_MEMBERS_ENDPOINT).build());
    when(requestBuilder.getRoleRightsUrl()).thenReturn(roleRightsUrl);
    when(requestBuilder.getRoleMembersPayload(any())).thenReturn(roleMembersPayload);
    when(requestBuilder.getRolesPayload(anyInt(), any(), any(), anySet(), any())).thenReturn(
        rolesPayload);
    when(executor.canConnect()).thenReturn(true, true, false);

    assertThrows(CyberArkPrivilegeCloudException.class,
        () -> getRequest.getRoles(0, "test role", true, null, new HashSet<>(), true,
            new HashMap<>()));
  }
}
