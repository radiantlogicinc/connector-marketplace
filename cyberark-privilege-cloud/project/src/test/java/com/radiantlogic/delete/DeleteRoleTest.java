package com.radiantlogic.delete;

import static com.radiantlogic.utility.Constants.DELETE_ROLES_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_ROLE_PAYLOAD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
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
class DeleteRoleTest {

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
  void deleteRole() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(DELETE_ROLES_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getOperationSuccessResponse());

    RequestBody requestBody = getDeleteRolePayload("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");

    when(executor.execute(any())).thenReturn(responseData);
    when(requestBuilder.getDeleteRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getDeleteRolePayload(any())).thenReturn(requestBody);

    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);

    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.SUCCESS);
  }

  @Test
  @SneakyThrows
  void deleteRoleWithInvalidCredentials() {
    when(executor.canConnect()).thenReturn(false);
    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteRoleWithBlankRDN() {
    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role= ,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteRoleWithException() {
    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenThrow(
        new CyberArkPrivilegeCloudException(""));

    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteRoleWithFailure() {
    HttpUrl httpUrl = TestUtils.getBaseUrl().addPathSegments(DELETE_ROLES_ENDPOINT).build();

    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);

    RequestBody requestBody = getDeleteRolePayload("be3c524a_1e5e_489b_b10a_69a9ebbfbc58");
    List<Record> roles =
        GOOGLE_JSON.fromJson(TestUtils.getResponseFromFile("role.json"), CyberArkResponse.class)
            .getResultData().getRecords();

    when(getRequest.getRoles(0, "Advanced TDR Monitoring", false, null, null, false,
        null)).thenReturn(
        roles);
    when(executor.execute(any())).thenReturn(responseData);
    when(executor.canConnect()).thenReturn(true);
    when(requestBuilder.getDeleteRoleUrl()).thenReturn(httpUrl);
    when(requestBuilder.getDeleteRolePayload(any())).thenReturn(requestBody);

    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteSystemAdministratorRole() {

    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=System Administrator,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  @Test
  @SneakyThrows
  void deleteRoleWithInvalidDetails() {

    LdapDeleteRequest ldapDeleteRequest =
        LdapDeleteRequest.create(DN.from("role=Advanced TDR Monitoring,ou=Roles,ou=CyberArk"));

    LdapResponse<String> response = deleteRequest.delete(ldapDeleteRequest);
    assertThat(response.getStatus()).isEqualTo(LdapResultCode.OPERATIONS_ERROR);
  }

  RequestBody getDeleteRolePayload(final String roleId) {
    String jsonString = String.format(DELETE_ROLE_PAYLOAD, roleId);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }
}
