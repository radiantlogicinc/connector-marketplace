package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.UPDATE_USER_ENDPOINT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
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
class UpdateUserTest {

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
    when(targetSchemaObjects.get(SCHEMA_OBJECTS_KEY)).thenReturn(Collections.singletonList("user"));
    when(schema.getSchemaObject(any())).thenReturn(Optional.of(TestUtils.getSchemaObjectForUser()));
  }

  @Test
  @SneakyThrows
  void updateUserDisplayName() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_USER_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateUserResponse = new ResponseData();
    updateUserResponse.setResponseCode(200);
    updateUserResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setId("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    userRecordRow.setDisplayName("new display name");

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(executor.execute(any())).thenReturn(updateUserResponse);
    when(requestBuilder.getUpdateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateUserPayload(any())).thenReturn(
        getUpdateUserPayload(userRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateUserMail() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_USER_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateUserResponse = new ResponseData();
    updateUserResponse.setResponseCode(200);
    updateUserResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setId("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    userRecordRow.setMail("a@g.com");

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(executor.execute(any())).thenReturn(updateUserResponse);
    when(requestBuilder.getUpdateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateUserPayload(any())).thenReturn(
        getUpdateUserPayload(userRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("user-mail", "a@g.com"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserMail() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_USER_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateUserResponse = new ResponseData();
    updateUserResponse.setResponseCode(200);
    updateUserResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setId("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    userRecordRow.setMail("");

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(executor.execute(any())).thenReturn(updateUserResponse);
    when(requestBuilder.getUpdateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateUserPayload(any())).thenReturn(
        getUpdateUserPayload(userRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("user-mail", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void updateUserMobile() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_USER_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateUserResponse = new ResponseData();
    updateUserResponse.setResponseCode(200);
    updateUserResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setId("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    userRecordRow.setMobile("+91112233");

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(executor.execute(any())).thenReturn(updateUserResponse);
    when(requestBuilder.getUpdateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateUserPayload(any())).thenReturn(
        getUpdateUserPayload(userRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("user-mobile", "+91112233"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserMobile() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(UPDATE_USER_ENDPOINT).build();
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    ResponseData updateUserResponse = new ResponseData();
    updateUserResponse.setResponseCode(200);
    updateUserResponse.setApiResponse(TestUtils.getOperationSuccessResponse());

    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setId("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    userRecordRow.setMobile("");

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(executor.execute(any())).thenReturn(updateUserResponse);
    when(requestBuilder.getUpdateUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getUpdateUserPayload(any())).thenReturn(
        getUpdateUserPayload(userRecordRow));

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("user-mobile", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserDisplayName() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("display-name", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserUuid() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);

    Modification modification = Modification.create(Modification.ModificationType.DELETE,
        Attribute.create("user-id", Lists.newArrayList()));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void addUserMobileWithTwoMobiles() {
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("user-mobile", Lists.newArrayList("123", "456")));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateInvalidUser() {
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateUserWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);

    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void updateUserWithInvalidRdnKey() {
    Modification modification = Modification.create(Modification.ModificationType.REPLACE,
        Attribute.create("display-name", "new display name"));

    LdapModifyRequest ldapModifyRequest = LdapModifyRequest.builder()
        .dn(DN.from("user1=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"))
        .modifications(Lists.newArrayList(modification)).build();

    LdapResponse<?> actual = updateRequest.modify(ldapModifyRequest);
    assertThat(actual.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  RequestBody getUpdateUserPayload(final RecordRow userRecordRow) {
    String jsonString = GOOGLE_JSON.toJson(userRecordRow);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

}
