package com.radiantlogic.delete;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.ldap.DN;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.util.TestUtil;
import lombok.SneakyThrows;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteUserTest {

  RequestBuilder requestBuilder;
  RequestExecutor executor;
  DeleteRequest deleteRequest;
  Request request;
  GetRequest getRequest;

  @BeforeEach
  void setUp() {
    requestBuilder = mock(RequestBuilder.class);
    executor = mock(RequestExecutor.class);
    getRequest = mock(GetRequest.class);
    deleteRequest = new DeleteRequest(requestBuilder, executor, getRequest);
    request = mock(Request.class);
    when(requestBuilder.canConnectWithValidToken()).thenReturn(true);
  }

  @Test
  @SneakyThrows
  void deleteUserWithSuccess() {
    ServiceNowResponse<UserRow> serviceNowResponse = TestUtil.getUsers();

    when(getRequest.getUsers(any(), any(), anyBoolean())).thenReturn(serviceNowResponse);
    when(requestBuilder.getDeleteUserRequest(any())).thenReturn(request);
    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=test_user_name,ou=Users,ou=ServiceNow"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    verify(executor).executeDelete(request);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteUserWithException() {
    ServiceNowResponse<UserRow> serviceNowResponse = TestUtil.getUsers();

    when(getRequest.getUsers(any(), any(), anyBoolean())).thenReturn(serviceNowResponse);
    when(requestBuilder.getDeleteUserRequest(any())).thenReturn(request);
    doThrow(new ServiceNowException("")).when(executor).executeDelete(any());
    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=test_user_name,ou=Users,ou=ServiceNow"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidCredentials() {
    when(requestBuilder.canConnectWithValidToken()).thenReturn(false);
    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user=test_user_name,ou=Users,ou=ServiceNow"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteUserWithInvalidDn() {
    LdapDeleteRequest ldapDeleteRequest = LdapDeleteRequest.create(
        DN.from("user1=test_user_name,ou=Users,ou=ServiceNow"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }
}
