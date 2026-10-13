package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.util.TestUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import lombok.SneakyThrows;
import okhttp3.Request;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateUserTest {

  RequestBuilder requestBuilder;

  RequestExecutor executor;
  ReadOnlyProperties targetSchemaObjects;
  Schema schema;
  UpdateRequestDispatcher updateRequestDispatcher;
  Request request;
  GetRequest getRequest;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    targetSchemaObjects = mock(ReadOnlyProperties.class);
    schema = mock(Schema.class);
    request = mock(Request.class);
    getRequest = mock(GetRequest.class);
    when(requestBuilder.getBatchSize()).thenReturn(40);
    updateRequestDispatcher =
        new UpdateRequestDispatcher(requestBuilder, executor, getRequest, targetSchemaObjects,
            schema, requestBuilder.getBatchSize());
    when(requestBuilder.canConnectWithValidToken()).thenReturn(true);
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtil.getSchemaObjectForUser()));
    doReturn(request).when(requestBuilder).getUpdateUserRequest(any(), any());
  }

  private LdapModifyRequest getModifyRequest(Modification modification) {
    return LdapModifyRequest.builder()
        .dn(DN.from("user=test_user_name,ou=Users,ou=ServiceNow"))
        .modifications(Lists.newArrayList(modification)).build();
  }

  private void mockCommonStubs(String fileName, int responseCode)
      throws IOException, ServiceNowException {
    ResponseData responseData = TestUtil.getUser(fileName, responseCode);
    ServiceNowResponse<UserRow> serviceNowResponse = TestUtil.getUsers();

    when(getRequest.getUsers(any(), any(), anyBoolean())).thenReturn(serviceNowResponse);
    when(executor.execute(any())).thenReturn(responseData);
  }

  private void mockGetUser() throws IOException, ServiceNowException {
    ServiceNowResponse<UserRow> serviceNowResponse = TestUtil.getUsers();
    when(getRequest.getUsers(any(), any(), anyBoolean())).thenReturn(serviceNowResponse);
  }

  @Test
  @SneakyThrows
  void testUpdateUserZipCode() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("zip", "92600"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserZipCodeWithBadRequest() {
    mockCommonStubs("", 400);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("zip", "92600"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);
    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserActiveStatus() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("active", "true"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserActiveStatusWithInvalidValue() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("active", "t"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserLockedOutStatus() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("LockedOut", "false"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserLockedOutStatusWithInvalidValue() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("LockedOut", "f"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserInternalIntegrationUserStatus() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("InternalIntegrationUser", "true"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserInternalIntegrationUserStatusWithInvalidValue() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("InternalIntegrationUser", "t"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserName() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("username", "test_user_name"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testDeleteUserName() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("username", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserSysId() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("sysid", "test_sys_id"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserFederatedId() {
    mockGetUser();

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("FederatedId", "test_federated_id"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void testUpdateUserGender() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("gender", "not specified"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserGenderWithInvalidValue() {
    mockCommonStubs("update_user_without_gender.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("gender", "not specified."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIntroduction() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("introduction", "mr."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIntroductionWithInvalidValue() {
    mockCommonStubs("update_user_without_introduction.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("introduction", "mr.."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIdentityType() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("IdentityType", "machine"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserIdentityTypeWithInvalidValue() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("IdentityType", "invalid_identity_type"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserCountry() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("country", "es"));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void testUpdateUserCountryWithInvalidValue() {
    mockCommonStubs("update_user.json", 200);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("country", "usa."));

    LdapModifyRequest ldapModifyRequest = getModifyRequest(modification);

    LdapResponse<?> actual = updateRequestDispatcher.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }
}
