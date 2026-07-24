package com.radiantlogic.util;

import static com.radiantlogic.utility.Constants.API_SUB_PATH;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_SECRET_KEY;
import static com.radiantlogic.utility.Constants.CONNECTION_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_ENDPOINT;
import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_KEY;
import static com.radiantlogic.utility.Constants.FORWARD_SLASH_KEY;
import static com.radiantlogic.utility.Constants.GET_ACCOUNTS_SUB_PATH;
import static com.radiantlogic.utility.Constants.GET_SAFES_SUB_PATH;
import static com.radiantlogic.utility.Constants.HOST_FOR_TESTING;
import static com.radiantlogic.utility.Constants.IDENTITY_TENANT_ID_KEY;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.LIMIT_KEY;
import static com.radiantlogic.utility.Constants.MEMBERS_SUB_PATH;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_KEY;
import static com.radiantlogic.utility.Constants.PASSWORD_VAULT_SUB_PATH;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.PORT_NUMBER_FOR_TESTING;
import static com.radiantlogic.utility.Constants.READ_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.RETRY_DELAY_KEY_NAME;
import static com.radiantlogic.utility.Constants.SCHEME_FOR_TESTING;
import static com.radiantlogic.utility.Constants.SORT_ACCOUNTS_FIELD_NAME;
import static com.radiantlogic.utility.Constants.SORT_KEY;
import static com.radiantlogic.utility.Constants.SUBDOMAIN_KEY;
import static com.radiantlogic.utility.Constants.URL_HOST_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.URL_SCHEME_KEY;
import static com.radiantlogic.utility.Constants.USE_CACHE_KEY;
import static com.radiantlogic.utility.Constants.WRITE_TIMEOUT_KEY_NAME;

import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.RoleRight;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings(PMD_KEY)
public final class TestUtils {

  private TestUtils() {
  }

  public static SchemaObject getSchemaObjectForUser() {
    Field field1 = new Field("name", "user-name", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("mail", "user-mail", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("mobilenumber", "user-mobile", new HashSet<>(), false, Field.Type.STRING);
    Field field4 =
        new Field("displayname", "display-name", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("id", "user-id", new HashSet<>(), false, Field.Type.STRING);
    Field field6 =
        new Field("risklevelrank", "risk-level", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("status", "user-status", new HashSet<>(), false, Field.Type.STRING);
    Field field8 = new Field("source", "user-source", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8));
    return new SchemaObject("user", "vdUser", "", "", "", Collections.singletonList("name"),
        new ArrayList<>(), fields);
  }

  public static SchemaObject getSchemaObjectForRole() {
    Field field1 = new Field("name", "role-name", new HashSet<>(), false, Field.Type.STRING);
    Field field2 =
        new Field("description", "role-description", new HashSet<>(), false, Field.Type.STRING);
    Field field3 = new Field("roletype", "role-type", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("members", "role-members", new HashSet<>(), false, Field.Type.STRING);
    Field field5 = new Field("id", "role-id", new HashSet<>(), false, Field.Type.STRING);
    Field field6 = new Field("rolerights", "", new HashSet<>(), false, Field.Type.STRING);
    List<Field> fields =
        new ArrayList<>(Arrays.asList(field1, field2, field3, field4, field5, field6));
    return new SchemaObject("role", "vdRole", "", "", "", Collections.singletonList("name"),
        new ArrayList<>(), fields);
  }

  public static String getResponseFromFile(final String file) {
    try {
      return IOUtils.toString(Objects.requireNonNull(
          Thread.currentThread().getContextClassLoader().getResourceAsStream(file)));
    } catch (IOException e) {
      return StringUtils.EMPTY;
    }
  }

  public static HttpUrl.Builder getBaseUrl() {
    return new HttpUrl.Builder().scheme(SCHEME_FOR_TESTING).host(HOST_FOR_TESTING)
        .port(PORT_NUMBER_FOR_TESTING);
  }

  private static HttpUrl.Builder getSubDomainEndpointsBaseUrl() {
    return new HttpUrl.Builder().scheme(SCHEME_FOR_TESTING).host(HOST_FOR_TESTING)
        .port(PORT_NUMBER_FOR_TESTING).addPathSegments(PASSWORD_VAULT_SUB_PATH);
  }

  public static HttpUrl getSafesUrl(String safeName, String nextLink, int pageSize) {
    HttpUrl safesUrl;
    if (StringUtils.isBlank(safeName)) {
      if (StringUtils.isNotBlank(nextLink)) {
        safesUrl =
            HttpUrl.parse(getSubDomainEndpointsBaseUrl().build() + FORWARD_SLASH_KEY + nextLink);
      } else {
        safesUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
            .addPathSegment(GET_SAFES_SUB_PATH)
            .addQueryParameter(LIMIT_KEY, String.valueOf(pageSize))
            .addQueryParameter(USE_CACHE_KEY, String.valueOf(Boolean.FALSE)).build();
      }
    } else {
      safesUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
          .addPathSegment(GET_SAFES_SUB_PATH).addPathSegment(safeName).build();
    }
    assert safesUrl != null;
    return safesUrl;
  }

  public static HttpUrl getSafeMembersUrl(String safeName, String nextLink, int pageSize) {
    HttpUrl safeMembersUrl;
    if (StringUtils.isNotBlank(nextLink)) {
      safeMembersUrl =
          HttpUrl.parse(getSubDomainEndpointsBaseUrl().build() + FORWARD_SLASH_KEY + nextLink);
    } else {
      safeMembersUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
          .addPathSegment(GET_SAFES_SUB_PATH).addPathSegments(safeName)
          .addPathSegments(MEMBERS_SUB_PATH).addQueryParameter(LIMIT_KEY, String.valueOf(pageSize))
          .addQueryParameter(USE_CACHE_KEY, String.valueOf(Boolean.FALSE)).build();
    }
    return safeMembersUrl;
  }

  public static SchemaObject getSchemaObjectForSafe() {
    Field field1 = new Field("name", "safe-name", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("number", "safe-number", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("description", "safe-description", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("members", "safe-members", new HashSet<>(), false, Field.Type.STRING);
    Field field5 =
        new Field("location", "safe-location", new HashSet<>(), false, Field.Type.STRING);
    Field field6 =
        new Field("managingCpm", "safe-managing-cpm", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("IsExpiredMember", "safe-Is-Expired-Member", new HashSet<>(), false,
        Field.Type.STRING);
    Field field8 =
        new Field("Created", "safe-Created-time", new HashSet<>(), false, Field.Type.STRING);
    Field field9 =
        new Field("Modified", "safe-Modified-time", new HashSet<>(), false, Field.Type.STRING);
    Field field10 = new Field("Creator", "safe-Creator", new HashSet<>(), false, Field.Type.STRING);
    Field field11 =
        new Field("IsOlacEnabled", "safe-IsOlacEnabled", new HashSet<>(), false, Field.Type.STRING);
    Field field12 =
        new Field("NumberOfVersionsRetention", "safe-NumberOfVersionsRetention", new HashSet<>(),
            false, Field.Type.STRING);
    Field field13 =
        new Field("NumberOfDaysRetention", "safe-NumberOfDaysRetention", new HashSet<>(), false,
            Field.Type.STRING);
    Field field14 =
        new Field("IsAutoPurgeEnabled", "safe-IsAutoPurgeEnabled", new HashSet<>(), false,
            Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10, field11, field12, field13, field14));
    return new SchemaObject("safe", "vdSafe", "", "", "", Collections.singletonList("name"),
        new ArrayList<>(), fields);
  }

  public static ReadOnlyProperties getConnectionProperties() {
    return ReadOnlyProperties.builder().put(IDENTITY_TENANT_ID_KEY, "test_tenant_id")
        .put(CLIENT_ID_KEY, "client_id@cyberark.cloud.39470")
        .put(CLIENT_SECRET_KEY, "client_secret")
        .put(PAGE_SIZE_KEY, 100).put(CONNECTION_TIMEOUT_KEY_NAME, 1).put(READ_TIMEOUT_KEY_NAME, 1)
        .put(WRITE_TIMEOUT_KEY_NAME, 1).put(RETRY_DELAY_KEY_NAME, 1).put(URL_SCHEME_KEY, "http")
        .put(URL_HOST_KEY, "localhost").put(URL_PORT_KEY, PORT_NUMBER_FOR_TESTING)
        .put(SUBDOMAIN_KEY, "test_subdomain").build();
  }

  public static ResponseData getSafe() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("safe.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getSafeMembersWithNextLink() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("safe_members_with_next_link.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getSafesWithoutNextLink() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("safes_without_next_link.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getSafesMembersWithoutNextLink() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("another_safe_members.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getSafeMemberWithoutNextLink() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(
        TestUtils.getResponseFromFile("safe_member_without_next_link.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static HttpUrl getAccountsUrl(String accountId, String nextLink, int pageSize) {
    HttpUrl accountsUrl;
    if (StringUtils.isBlank(accountId)) {
      if (StringUtils.isNotBlank(nextLink)) {
        accountsUrl =
            HttpUrl.parse(getSubDomainEndpointsBaseUrl().build() + FORWARD_SLASH_KEY + nextLink);
      } else {
        accountsUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
            .addPathSegment(GET_ACCOUNTS_SUB_PATH)
            .addQueryParameter(LIMIT_KEY, String.valueOf(pageSize))
            .addQueryParameter(SORT_KEY, SORT_ACCOUNTS_FIELD_NAME)
            .addQueryParameter(USE_CACHE_KEY, String.valueOf(Boolean.FALSE)).build();
      }
    } else {
      accountsUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
          .addPathSegment(GET_ACCOUNTS_SUB_PATH).addPathSegment(accountId).build();
    }
    Objects.requireNonNull(accountsUrl, "accountsUrl must not be null");
    return accountsUrl;
  }

  public static ResponseData getAccountsWithNextLink() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("accounts_with_next_link.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getAccountsWithoutNextLink() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("accounts_without_next_link.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getAccount() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("account.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getDeletedAccount() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("account_with_deletion_time.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getAccountsWithBadRequest() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);
    return responseData;
  }

  public static SchemaObject getSchemaObjectForAccount() {
    Field field1 = new Field("name", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("id", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("address", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("username", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 =
        new Field("platformid", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 =
        new Field("safeName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("secretType", "", new HashSet<>(), false,
        Field.Type.STRING);
    Field field8 =
        new Field("createdTime", "", new HashSet<>(), false, Field.Type.STRING);
    Field field9 =
        new Field("secretManagement", "", new HashSet<>(), false, Field.Type.STRING);
    Field field10 =
        new Field("platformAccountProperties", "", new HashSet<>(), false, Field.Type.STRING);
    Field field11 =
        new Field("categoryModificationTime", "", new HashSet<>(), false, Field.Type.STRING);
    Field field12 =
        new Field("remoteMachinesAccess", "", new HashSet<>(),
            false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10, field11, field12));
    return new SchemaObject("account", "vdAccount", "", "", "", Collections.singletonList("id"),
        new ArrayList<>(), fields);
  }

  public static ResponseData getCreateRoleResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("create_role.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getRolesResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("roles.json"));
    return responseData;
  }

  public static ResponseData getAnotherRoleResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("another_role.json"));
    return responseData;
  }

  public static ResponseData getRolesWithMembersResponse() {
    ResponseData rolesWithMembersResponse = new ResponseData();
    rolesWithMembersResponse.setResponseCode(200);
    rolesWithMembersResponse.setApiResponse(
        TestUtils.getResponseFromFile("roles_with_members.json"));
    return rolesWithMembersResponse;
  }

  public static ResponseData getEmptyRolesResponse() {
    ResponseData emptyRolesResponse = new ResponseData();
    emptyRolesResponse.setResponseCode(200);
    emptyRolesResponse.setApiResponse(
        TestUtils.getResponseFromFile("empty_roles_with_members.json"));
    return emptyRolesResponse;
  }

  public static ResponseData getUsersResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("users.json"));
    return responseData;

  }

  public static String getOperationSuccessResponse() {
    return getResponseFromFile("operation_success.json");
  }

  public static HttpUrl getDirectoryServicesUrl() {
    return getBaseUrl().addPathSegments(DIRECTORY_SERVICES_ENDPOINT).build();
  }

  public static ResponseData getDirectoryServicesResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setApiResponse(TestUtils.getResponseFromFile("directory_services.json"));
    responseData.setResponseCode(200);
    return responseData;
  }

  public static ResponseData getDirectoryServicesResponseWithBadRequest() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(400);
    return responseData;
  }

  public static RequestBody getDirectoryServicesRequestBody() {
    return RequestBody.create("{}", JSON_MEDIA_TYPE);
  }

  public static SchemaObject getSchemaObjectForDirectoryService() {
    Field field1 = new Field("Service", "", new HashSet<>(), false, Field.Type.STRING);
    Field field2 = new Field("DisplayName", "", new HashSet<>(), false, Field.Type.STRING);
    Field field3 =
        new Field("Tenant", "", new HashSet<>(), false, Field.Type.STRING);
    Field field4 = new Field("Name", "", new HashSet<>(), false, Field.Type.STRING);
    Field field5 =
        new Field("Status", "", new HashSet<>(), false, Field.Type.STRING);
    Field field6 =
        new Field("Config", "", new HashSet<>(), false, Field.Type.STRING);
    Field field7 = new Field("SyncRulesConfigured", "", new HashSet<>(), false,
        Field.Type.STRING);
    Field field8 =
        new Field("StatusDisplay", "", new HashSet<>(), false, Field.Type.STRING);
    Field field9 =
        new Field("Everybody", "", new HashSet<>(), false, Field.Type.STRING);
    Field field10 =
        new Field("SyncJobStatus", "", new HashSet<>(), false, Field.Type.STRING);
    Field field11 =
        new Field("DisplayNameShort", "", new HashSet<>(), false, Field.Type.STRING);
    Field field12 =
        new Field("DirectoryServiceUuid", "", new HashSet<>(),
            false, Field.Type.STRING);
    Field field13 =
        new Field("Description", "", new HashSet<>(),
            false, Field.Type.STRING);
    List<Field> fields = new ArrayList<>(
        Arrays.asList(field1, field2, field3, field4, field5, field6, field7, field8, field9,
            field10, field11, field12, field13));
    return new SchemaObject(DIRECTORY_SERVICES_KEY, "vdDirectoryservice", "", "", "",
        Collections.singletonList("name"),
        new ArrayList<>(), fields);
  }

  public static RequestBody getRoleRightsPayload(String roleId) {
    String jsonString = String.format("{\n"
        + "    \"role\": \"%s\"\n"
        + "}", roleId);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  public static ResponseData getRoleRightsResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("role_rights.json"));
    return responseData;
  }

  public static ResponseData getEmptyRoleRightsResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("empty_role_rights.json"));
    return responseData;
  }

  public static ResponseData getRoleRightsWithInvalidResponse() {
    ResponseData responseData = new ResponseData();
    responseData.setResponseCode(200);
    responseData.setApiResponse(TestUtils.getResponseFromFile("invalid_role_rights_response.json"));
    return responseData;
  }

  public static void assignRoleRights(CyberArkResponse cyberArkResponse, Record role) {
    List<RoleRight> roleRights =
        cyberArkResponse.getResultData().getRecords().stream()
            .filter(record -> record != null && record.getRecordRow() != null)
            .map(Record::getRecordRow)
            .map(row -> {
              RoleRight roleRight = new RoleRight();
              roleRight.setPath(row.getPath());
              roleRight.setScope(row.getScope());
              roleRight.setDescription(row.getDescription());
              roleRight.setServiceName(row.getServiceName());
              return roleRight;
            })
            .collect(Collectors.toList());
    role.setRoleRights(roleRights);
  }

  public static ReadOnlyProperties getConnectionPropertiesWithoutSubDomain() {
    return ReadOnlyProperties.builder().put(IDENTITY_TENANT_ID_KEY, "test_tenant_id")
        .put(CLIENT_ID_KEY, "client_id@cyberark.cloud.39470")
        .put(CLIENT_SECRET_KEY, "client_secret")
        .put(PAGE_SIZE_KEY, 100).put(CONNECTION_TIMEOUT_KEY_NAME, 1).put(READ_TIMEOUT_KEY_NAME, 1)
        .put(WRITE_TIMEOUT_KEY_NAME, 1).put(RETRY_DELAY_KEY_NAME, 1).put(URL_SCHEME_KEY, "http")
        .put(URL_HOST_KEY, "localhost").put(URL_PORT_KEY, PORT_NUMBER_FOR_TESTING).build();
  }
}
