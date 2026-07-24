package com.radiantlogic.delete;

import static com.radiantlogic.utility.Constants.DELETE_USERS_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_USER_PAYLOAD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.util.TestUtils;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import java.util.List;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings(PMD_KEY)
class DeleteUserTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  GetRequest getRequest;
  DeleteRequest deleteRequest;

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    getRequest = mock(GetRequest.class);
    deleteRequest = new DeleteRequest(requestBuilder, getRequest, executor);
    when(executor.canConnect()).thenReturn(true);
    when(executor.getTokenWithBearerPrefixed()).thenReturn("Bearer ey..");
  }

  @Test
  @SneakyThrows
  void deleteUser() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(DELETE_USERS_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getOperationSuccessResponse());

    RequestBody requestBody = getDeleteUserPayload("8d64055b-1234-4550-9dbb-5c8f2bce35db");

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getDeleteUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getDeleteUserPayload(any())).thenReturn(requestBody);

    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(requestBuilder.getClientId()).thenReturn("test-client-id");
    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserWithBlankRDN() {
    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("user= ,ou=Users,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidCredentials() {
    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"));

    when(executor.canConnect()).thenReturn(false);

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(DELETE_USERS_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    RequestBody requestBody = getDeleteUserPayload("8d64055b-1234-4550-9dbb-5c8f2bce35db");
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(requestBuilder.getClientId()).thenReturn("test-client-id");
    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);
    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getDeleteUserUrl()).thenReturn(httpUrl);
    when(requestBuilder.getDeleteUserPayload(any())).thenReturn(requestBody);
    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithSameUserToBeDeletedWithWhichConnectorConfigured() {
    when(requestBuilder.getClientId()).thenReturn("abcc1s232@cyberark.cloud.39470");
    List<Record> users =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("user.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getUsers(0, "abcc1s232@cyberark.cloud.39470", null, null, null)).thenReturn(
        users);

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidDetails() {
    when(requestBuilder.getClientId()).thenReturn("test-client-id");

    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=abcc1s232@cyberark.cloud.39470,ou=Users,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  RequestBody getDeleteUserPayload(final String userId) {
    String jsonString = String.format(DELETE_USER_PAYLOAD, userId);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }
}
