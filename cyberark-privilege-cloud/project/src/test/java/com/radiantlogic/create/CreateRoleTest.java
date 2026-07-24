package com.radiantlogic.create;

import static com.radiantlogic.utility.Constants.CREATE_ROLE_ENDPOINT;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.RolePojo;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class CreateRoleTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  GetRequest getRequest;
  CreateRequest createRequest;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;

  static final String DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED = "Bearer ey..";
  static final String ROLE_NAME_KEY = "role-name";
  static final String ROLE_DESCRIPTION_KEY = "role-description";
  static final String ROLE_TYPE_KEY = "role-type";
  static final String ROLE_MEMBERS_KEY = "role-members";

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    getRequest = mock(GetRequest.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);

    createRequest =
        new CreateRequest(requestBuilder, getRequest, executor, targetSchemaObjects, schema);
    when(executor.canConnect()).thenReturn(true);
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("role"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForRole()));
  }

  @Test
  @SneakyThrows
  void createRole() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_ROLE_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData responseData = TestUtils.getCreateRoleResponse();

    when(requestBuilder.getCreateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateRolePayload(any())).thenReturn(getCreateRoleRequestBody());
    when(getRequest.getUsers(anyInt(), any(), any(), anySet(), any())).thenReturn(users);
    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), anySet(),
        anyBoolean(), any())).thenReturn(roles);
    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void createRoleWithScriptRoleTypeWithNoMembers() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_ROLE_ENDPOINT).build();

    ResponseData responseData = TestUtils.getCreateRoleResponse();

    when(requestBuilder.getCreateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateRolePayload(any())).thenReturn(getCreateRoleRequestBody());
    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_TYPE_KEY, "script"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void createRoleWithScriptRoleTypeWithMembers() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getUsers(anyInt(), any(), any(), anySet(), any())).thenReturn(users);
    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), anySet(),
        anyBoolean(), any())).thenReturn(roles);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "script"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void createRoleWithCredentialsBecameInvalidDuringRunTime() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(executor.canConnect()).thenReturn(true, false);
    when(getRequest.getUsers(anyInt(), any(), any(), anySet(), any())).thenReturn(users);
    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), anySet(),
        anyBoolean(), any())).thenReturn(roles);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }


  @Test
  @SneakyThrows
  void createRoleWithUniqueKeyAndRdnNotSame() {

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name1,ou=Roles,ou=CyberArk"))
            .attributes(Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void createRoleWithInvalidMemberUuid() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> roles = GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("empty_roles.json"),
        CyberArkResponse.class).getResultData().getRecords();

    when(getRequest.getUsers(anyInt(), any(), any(), anySet(), any())).thenReturn(users);
    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), anySet(),
        anyBoolean(), any())).thenReturn(roles);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateRoleWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(CREATE_ROLE_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    when(requestBuilder.getCreateRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getCreateRolePayload(any())).thenReturn(getCreateRoleRequestBody());
    when(getRequest.getUsers(anyInt(), any(), any(), anySet(), any())).thenReturn(users);
    when(getRequest.getRoles(anyInt(), any(), anyBoolean(), any(), anySet(),
        anyBoolean(), any())).thenReturn(roles);
    when(executor.execute(any())).thenReturn(responseData);
    when(executor.getTokenWithBearerPrefixed()).thenReturn(DUMMY_ACCESS_TOKEN_WITH_BEARER_PREFIXED);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateRoleWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=test role name,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testCreateRoleWithBlankRdnUniqueKey() {

    final LdapAddRequest ldapAddRequest =
        LdapAddRequest.builder().dn(DN.from("role=,ou=Roles,ou=CyberArk")).attributes(
            Lists.newArrayList(Attribute.create(ROLE_NAME_KEY, "test role name"),
                Attribute.create(ROLE_DESCRIPTION_KEY, "role desc"),
                Attribute.create(ROLE_MEMBERS_KEY,
                    Lists.newArrayList("8d64055b-1234-4550-9dbb-5c8f2bce35db",
                        "[be3c524a_1e5e_489b_b10a_69a9ebbfbc58]")),
                Attribute.create(ROLE_TYPE_KEY, "principallist"))).build();

    LdapResponse<?> actual = createRequest.create(ldapAddRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  public static RequestBody getCreateRoleRequestBody() {
    RolePojo rolePojo = new RolePojo();
    rolePojo.setName("test role name");
    rolePojo.setDescription("role desc");
    rolePojo.setRoleType("PrincipalList");
    rolePojo.setUsers(Collections.singleton("8d64055b-1234-4550-9dbb-5c8f2bce35db"));
    rolePojo.setRoles(Collections.singleton("be3c524a_1e5e_489b_b10a_69a9ebbfbc58"));
    return RequestBody.create(GOOGLE_JSON.toJson(rolePojo), JSON_MEDIA_TYPE);
  }
}
