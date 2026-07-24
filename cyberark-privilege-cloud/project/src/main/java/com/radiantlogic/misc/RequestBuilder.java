package com.radiantlogic.misc;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.API_SUB_PATH;
import static com.radiantlogic.utility.Constants.BASE_URL_PREFIX;
import static com.radiantlogic.utility.Constants.CLIENT_CREDENTIALS_GRANT_TYPE;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_SECRET_KEY;
import static com.radiantlogic.utility.Constants.CONTENT_TYPE_KEY;
import static com.radiantlogic.utility.Constants.CREATE_ROLE_ENDPOINT;
import static com.radiantlogic.utility.Constants.CREATE_USER_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_ROLES_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_ROLE_PAYLOAD;
import static com.radiantlogic.utility.Constants.DELETE_USERS_ENDPOINT;
import static com.radiantlogic.utility.Constants.DELETE_USER_PAYLOAD;
import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_ENDPOINT;
import static com.radiantlogic.utility.Constants.FORWARD_SLASH_KEY;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.GET_ACCOUNTS_SUB_PATH;
import static com.radiantlogic.utility.Constants.GET_ROLES_BY_IDS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLES_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLES_WITH_MEMBERS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLE_BY_ID_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLE_BY_NAME_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_ROLE_MEMBERS_ENDPOINT;
import static com.radiantlogic.utility.Constants.GET_ROLE_MEMBERS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_SAFES_SUB_PATH;
import static com.radiantlogic.utility.Constants.GET_USERS_BY_IDS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_USERS_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_USER_BY_ID_PAYLOAD;
import static com.radiantlogic.utility.Constants.GET_USER_BY_USERNAME_PAYLOAD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON_FOR_SERIALIZATION;
import static com.radiantlogic.utility.Constants.GRANT_TYPE_KEY;
import static com.radiantlogic.utility.Constants.IDENTITY_TENANT_ID_KEY;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.LIMIT_KEY;
import static com.radiantlogic.utility.Constants.MEMBERS_SUB_PATH;
import static com.radiantlogic.utility.Constants.MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_KEY;
import static com.radiantlogic.utility.Constants.PASSWORD_VAULT_SUB_PATH;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.RED_ROCK_ENDPOINT;
import static com.radiantlogic.utility.Constants.ROLE_RIGHTS_ENDPOINT;
import static com.radiantlogic.utility.Constants.SAFE_NAME_FILTER_PREFIX;
import static com.radiantlogic.utility.Constants.SORT_ACCOUNTS_FIELD_NAME;
import static com.radiantlogic.utility.Constants.SORT_KEY;
import static com.radiantlogic.utility.Constants.SORT_SAFES_FIELD_NAME;
import static com.radiantlogic.utility.Constants.SORT_SAFE_MEMBERS_FIELD_NAME;
import static com.radiantlogic.utility.Constants.SUBDOMAIN_BASE_URL_PREFIX;
import static com.radiantlogic.utility.Constants.SUBDOMAIN_KEY;
import static com.radiantlogic.utility.Constants.SUBDOMAIN_NOT_AVAILABLE_ERROR_MESSAGE;
import static com.radiantlogic.utility.Constants.UPDATE_ROLE_ENDPOINT;
import static com.radiantlogic.utility.Constants.UPDATE_USER_ENDPOINT;
import static com.radiantlogic.utility.Constants.URL_ENCODED_CONTENT_TYPE;
import static com.radiantlogic.utility.Constants.URL_HOST_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.URL_SCHEME;
import static com.radiantlogic.utility.Constants.URL_SCHEME_KEY;
import static com.radiantlogic.utility.Constants.USE_CACHE_KEY;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.RolePojo;
import com.radiantlogic.utility.RoleUtils;
import com.radiantlogic.utility.UserUtils;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

/**
 * This class provide request url/payload for various CRUD operations.
 */
@Slf4j
@Getter
@SuppressWarnings(PMD_KEY)
public class RequestBuilder {

  /**
   * This will store identity tenant id's value as part of connector configuration parameter.
   */
  private final String identityTenantId;
  /**
   * This will store client id's value as part of connector configuration parameter.
   */
  private final String clientId;
  /**
   * This will store client secret's value as part of connector configuration parameter.
   */
  private final String clientSecret;
  /**
   * This field will store subdomain's value.
   */
  private String subdomain;
  /**
   * This will store page size value as part of connector configuration parameter.
   */
  private final int pageSize;
  /**
   * This field will contain an optional url scheme.
   */
  private String scheme;
  /**
   * This field will contain an optional url host.
   */
  private String host;
  /**
   * This field will contain an optional url port.
   */
  private Long port;

  /**
   * This constructor will initialize RequestBuilder class's instance members.
   *
   * @param connectionProperties Contains values to be used for members initialization
   */
  public RequestBuilder(@Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                        final ReadOnlyProperties connectionProperties) {
    identityTenantId = Objects.requireNonNull(connectionProperties.get(IDENTITY_TENANT_ID_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, IDENTITY_TENANT_ID_KEY))
        .toString();
    clientId = Objects.requireNonNull(connectionProperties.get(CLIENT_ID_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, CLIENT_ID_KEY)).toString();
    clientSecret = Objects.requireNonNull(connectionProperties.get(CLIENT_SECRET_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, CLIENT_SECRET_KEY)).toString();
    pageSize = Integer.parseInt(Objects.requireNonNull(connectionProperties.get(PAGE_SIZE_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, PAGE_SIZE_KEY)).toString());
    initializeAdditionalFields(connectionProperties);
  }

  private void initializeAdditionalFields(final ReadOnlyProperties connectionProperties) {
    if (connectionProperties.get(SUBDOMAIN_KEY) != null) {
      subdomain = String.valueOf(connectionProperties.get(SUBDOMAIN_KEY));
    }
    if (connectionProperties.get(URL_SCHEME_KEY) != null) {
      scheme = String.valueOf(connectionProperties.get(URL_SCHEME_KEY));
    }
    if (connectionProperties.get(URL_HOST_KEY) != null) {
      host = String.valueOf(connectionProperties.get(URL_HOST_KEY));
    }
    if (connectionProperties.get(URL_PORT_KEY) != null) {
      port = ((Number) connectionProperties.get(URL_PORT_KEY)).longValue();
    }
  }

  /**
   * This method will create generate token Request object.
   *
   * @return Request object
   */
  public Request getTokenRequest() {
    return new Request.Builder().url(getAccessTokenUrl()).post(getFormBodyForAccessToken())
        .addHeader(CONTENT_TYPE_KEY, URL_ENCODED_CONTENT_TYPE).build();
  }

  private HttpUrl getAccessTokenUrl() {
    HttpUrl tokenUrl = getBaseUrl().addPathSegments(GENERATE_ACCESS_TOKEN_ENDPOINT).build();
    log.debug("url to generate access token is:{}", tokenUrl.url());
    return tokenUrl;
  }

  /**
   * This method will create base url.
   *
   * @return HttpUrl.Builder containing base url
   */
  public HttpUrl.Builder getBaseUrl() {
    HttpUrl.Builder builder = new HttpUrl.Builder().scheme(scheme != null ? scheme : URL_SCHEME)
        .host(host != null ? host : String.format(BASE_URL_PREFIX, identityTenantId));
    if (port != null) {
      builder.port(port.intValue());
    }
    return builder;
  }

  private FormBody getFormBodyForAccessToken() {
    return new FormBody.Builder().addEncoded(GRANT_TYPE_KEY, CLIENT_CREDENTIALS_GRANT_TYPE)
        .addEncoded(CLIENT_ID_KEY, clientId).addEncoded(CLIENT_SECRET_KEY, clientSecret).build();
  }

  /**
   * This method will create red rock query url.
   *
   * @return HttpUrl
   */
  public HttpUrl getRedRockUrl() {
    HttpUrl redRockUrl = getBaseUrl().addPathSegments(RED_ROCK_ENDPOINT).build();
    log.debug("red rock url is:{}", redRockUrl.url());
    return redRockUrl;
  }

  /**
   * This method will construct get roles payload.
   *
   * @param pageNumber page number for pagination
   * @param roleName   role name for whom details to be fetched
   * @param roleId     role id for whom details to be fetched
   * @param roleIds    role ids for whom details to be fetched
   * @param filters    Filters to be applied while getting roles
   * @return RequestBody
   */
  public RequestBody getRolesPayload(final int pageNumber, final String roleName,
                                     final String roleId, final Set<String> roleIds,
                                     Map<String, String> filters) {
    String jsonString;
    if (StringUtils.isNotBlank(roleName)) {
      jsonString = String.format(GET_ROLE_BY_NAME_PAYLOAD, roleName);
    } else if (StringUtils.isNotBlank(roleId)) {
      jsonString = String.format(GET_ROLE_BY_ID_PAYLOAD, roleId);
    } else if (roleIds != null && !roleIds.isEmpty()) {
      jsonString = String.format(GET_ROLES_BY_IDS_PAYLOAD,
          roleIds.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
    } else if (filters != null && !filters.isEmpty()) {
      jsonString = RoleUtils.getPayloadWithFilters(pageNumber, pageSize, filters);
    } else {
      jsonString = String.format(GET_ROLES_PAYLOAD, pageNumber, pageSize, pageSize);
    }
    log.debug("get roles payload is: {}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct get role members url.
   *
   * @return HttpUrl
   */
  public HttpUrl getRoleMembersUrl() {
    HttpUrl roleMembersUrl = getBaseUrl().addPathSegments(GET_ROLE_MEMBERS_ENDPOINT).build();
    log.debug("get role members url is:{}", roleMembersUrl.url());
    return roleMembersUrl;
  }

  /**
   * This method will construct get role members payload.
   *
   * @param roleId role id for whom members to be fetched
   * @return RequestBody
   */
  public RequestBody getRoleMembersPayload(final String roleId) {
    String jsonString = String.format(GET_ROLE_MEMBERS_PAYLOAD, roleId);

    log.debug("get role members payload is: {}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct get role with members payload.
   *
   * @param pageNumber page number for pagination
   * @return RequestBody
   */
  public RequestBody getRolesWithMembersListPayload(final int pageNumber) {
    String jsonString =
        String.format(GET_ROLES_WITH_MEMBERS_PAYLOAD, pageNumber, pageSize, pageSize);
    log.debug("get roles with members list payload is: {}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct get users payload.
   *
   * @param pageNumber page number for pagination
   * @param username   username for whom details to be fetched
   * @param userId     user id for whom details to be fetched
   * @param userIds    user ids for whom details to be fetched
   * @param filters    Filters to be applied while getting users
   * @return RequestBody
   */
  public RequestBody getUsersPayload(final int pageNumber, final String username,
                                     final String userId, final Set<String> userIds,
                                     Map<String, String> filters) {
    String jsonString;
    if (StringUtils.isNotBlank(username)) {
      jsonString = String.format(GET_USER_BY_USERNAME_PAYLOAD, username);
    } else if (StringUtils.isNotBlank(userId)) {
      jsonString = String.format(GET_USER_BY_ID_PAYLOAD, userId);
    } else if (userIds != null && !userIds.isEmpty()) {
      jsonString = String.format(GET_USERS_BY_IDS_PAYLOAD,
          userIds.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
    } else if (filters != null && !filters.isEmpty()) {
      jsonString = UserUtils.getPayloadWithFilters(pageNumber, pageSize, filters);
    } else {
      jsonString = String.format(GET_USERS_PAYLOAD, pageNumber, pageSize, pageSize);
    }
    log.debug("get user(s) payload is: {}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct delete role url.
   *
   * @return HttpUrl
   */
  public HttpUrl getDeleteRoleUrl() {
    HttpUrl deleteRoleUrl = getBaseUrl().addPathSegments(DELETE_ROLES_ENDPOINT).build();
    log.debug("delete role url is:{}", deleteRoleUrl.url());
    return deleteRoleUrl;
  }

  /**
   * This method will construct delete role payload.
   *
   * @param roleId role id of role to be deleted
   * @return RequestBody
   */
  public RequestBody getDeleteRolePayload(final String roleId) {
    String jsonString = String.format(DELETE_ROLE_PAYLOAD, roleId);
    log.debug("delete role payload is: {}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct delete user url.
   *
   * @return HttpUrl
   */
  public HttpUrl getDeleteUserUrl() {
    HttpUrl deleteUserUrl = getBaseUrl().addPathSegments(DELETE_USERS_ENDPOINT).build();
    log.debug("delete user url is:{}", deleteUserUrl.url());
    return deleteUserUrl;
  }

  /**
   * This method will construct delete user payload.
   *
   * @param userId user id of user to be deleted
   * @return RequestBody
   */
  public RequestBody getDeleteUserPayload(final String userId) {
    String jsonString = String.format(DELETE_USER_PAYLOAD, userId);
    log.debug("delete user payload is: {}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct update user url.
   *
   * @return HttpUrl
   */
  public HttpUrl getUpdateUserUrl() {
    HttpUrl updateUserUrl = getBaseUrl().addPathSegments(UPDATE_USER_ENDPOINT).build();
    log.debug("update user url is:{}", updateUserUrl.url());
    return updateUserUrl;
  }

  /**
   * This method will construct update user payload.
   *
   * @param userRecordRow Object containing user details to be updated
   * @return RequestBody
   */
  public RequestBody getUpdateUserPayload(final RecordRow userRecordRow) {
    String jsonString = GOOGLE_JSON_FOR_SERIALIZATION.toJson(userRecordRow);
    log.debug("update user payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct update role payload.
   *
   * @return HttpUrl
   */
  public HttpUrl getUpdateRoleUrl() {
    HttpUrl updateRoleUrl = getBaseUrl().addPathSegments(UPDATE_ROLE_ENDPOINT).build();
    log.debug("update role url is:{}", updateRoleUrl.url());
    return updateRoleUrl;
  }

  /**
   * This method will construct update role payload.
   *
   * @param userRecordRow role object containing details to be updated
   * @return RequestBody
   */
  public RequestBody getUpdateRolePayload(final RecordRow userRecordRow) {
    String jsonString = GOOGLE_JSON_FOR_SERIALIZATION.toJson(userRecordRow);
    log.debug("update role payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct create role url.
   *
   * @return HttpUrl
   */
  public HttpUrl getCreateRoleUrl() {
    HttpUrl createRoleUrl = getBaseUrl().addPathSegments(CREATE_ROLE_ENDPOINT).build();
    log.debug("create role url is:{}", createRoleUrl.url());
    return createRoleUrl;
  }

  /**
   * This method will construct create role payload.
   *
   * @param rolePojo Object containing details of role object to be created
   * @return RequestBody
   */
  public RequestBody getCreateRolePayload(final RolePojo rolePojo) {
    String jsonString = GOOGLE_JSON_FOR_SERIALIZATION.toJson(rolePojo);
    log.debug("create role payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * This method will construct create user url.
   *
   * @return HttpUrl
   */
  public HttpUrl getCreateUserUrl() {
    HttpUrl createUserUrl = getBaseUrl().addPathSegments(CREATE_USER_ENDPOINT).build();
    log.debug("create user url is:{}", createUserUrl.url());
    return createUserUrl;
  }

  /**
   * This method will construct create user payload.
   *
   * @param userRecordRow User object containing user details
   * @return RequestBody
   */
  public RequestBody getCreateUserPayload(final RecordRow userRecordRow) {
    String jsonString = GOOGLE_JSON_FOR_SERIALIZATION.toJson(userRecordRow);
    log.debug("create user payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  private HttpUrl.Builder getSubDomainEndpointsBaseUrl() throws CyberArkPrivilegeCloudException {
    if (StringUtils.isBlank(subdomain)) {
      throw new CyberArkPrivilegeCloudException(SUBDOMAIN_NOT_AVAILABLE_ERROR_MESSAGE);
    }
    HttpUrl.Builder builder =
        new HttpUrl.Builder().scheme(scheme != null ? scheme : URL_SCHEME)
            .host(host != null ? host : String.format(SUBDOMAIN_BASE_URL_PREFIX, subdomain));
    if (port != null) {
      builder.port(port.intValue());
    } else {
      builder.addPathSegments(PASSWORD_VAULT_SUB_PATH);
    }
    return builder;
  }

  /**
   * This method will return get safe(s) url.
   *
   * @param safeName If a safe name is provided, then url to get that safe will be returned,
   *                 otherwise url to get all safes will be returned
   * @param nextLink Next link used in pagination
   * @param filters  Filters to be applied while getting safes
   * @return Safe(s) url
   * @throws CyberArkPrivilegeCloudException In case subdomain is not available.
   */
  public HttpUrl getSafesUrl(String safeName, String nextLink, Map<String, String> filters)
      throws CyberArkPrivilegeCloudException {
    HttpUrl safesUrl;
    if (StringUtils.isBlank(safeName)) {
      if (StringUtils.isNotBlank(nextLink)) {
        safesUrl =
            HttpUrl.parse(getSubDomainEndpointsBaseUrl().build() + FORWARD_SLASH_KEY + nextLink);
      } else {
        safesUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
            .addPathSegment(GET_SAFES_SUB_PATH)
            .addQueryParameter(LIMIT_KEY, String.valueOf(pageSize))
            .addQueryParameter(SORT_KEY, SORT_SAFES_FIELD_NAME)
            .addQueryParameter(USE_CACHE_KEY, String.valueOf(Boolean.FALSE))
            .build();
        if (filters != null && !filters.isEmpty()) {
          String keyName = filters.entrySet().iterator().next().getKey();
          safesUrl =
              safesUrl.newBuilder().addQueryParameter(keyName, filters.get(keyName)).build();
        }
      }
    } else {
      safesUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
          .addPathSegment(GET_SAFES_SUB_PATH).addPathSegment(safeName).build();
    }
    Objects.requireNonNull(safesUrl, "safesUrl must not be null");
    log.debug("get safe(s) url is:{}", safesUrl.url());
    return safesUrl;
  }

  /**
   * This method will return get safe members url.
   *
   * @param safeName Safe name for whom members to be fetched
   * @param nextLink Next link used in pagination
   * @return Safe members url
   * @throws CyberArkPrivilegeCloudException In case subdomain is not available.
   */
  public HttpUrl getSafeMembersUrl(String safeName, String nextLink)
      throws CyberArkPrivilegeCloudException {
    HttpUrl safeMembersUrl;
    if (StringUtils.isNotBlank(nextLink)) {
      safeMembersUrl =
          HttpUrl.parse(getSubDomainEndpointsBaseUrl().build() + FORWARD_SLASH_KEY + nextLink);
    } else {
      safeMembersUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
          .addPathSegment(GET_SAFES_SUB_PATH).addPathSegments(safeName)
          .addPathSegments(MEMBERS_SUB_PATH).addQueryParameter(LIMIT_KEY, String.valueOf(pageSize))
          .addQueryParameter(SORT_KEY, SORT_SAFE_MEMBERS_FIELD_NAME)
          .addQueryParameter(USE_CACHE_KEY, String.valueOf(Boolean.FALSE)).build();
    }
    log.debug("get safe members url is:{}", safeMembersUrl);
    return safeMembersUrl;
  }

  /**
   * This method will return get account(s) url.
   *
   * @param accountId If an account id is provided, then url to get that account will be returned,
   *                  otherwise for all accounts will be returned
   * @param nextLink  Next link used in pagination
   * @param filters   Filters to be applied while getting accounts
   * @return Account(s) url
   * @throws CyberArkPrivilegeCloudException In case subdomain is not available.
   */
  public HttpUrl getAccountsUrl(String accountId, String nextLink,
                                Map<String, String> filters)
      throws CyberArkPrivilegeCloudException {
    HttpUrl url;
    if (StringUtils.isBlank(accountId)) {
      url = getAccountsUrl(nextLink, filters);
    } else {
      url = getAccountUrl(accountId);
    }
    log.debug("get account(s) url is:{}", url.url());
    return url;
  }

  @NonNull
  private HttpUrl getAccountsUrl(String nextLink, Map<String, String> filters)
      throws CyberArkPrivilegeCloudException {
    HttpUrl accountsUrl;
    if (StringUtils.isNotBlank(nextLink)) {
      accountsUrl =
          HttpUrl.parse(getSubDomainEndpointsBaseUrl().build() + FORWARD_SLASH_KEY + nextLink);
      if (accountsUrl == null) {
        throw new IllegalStateException("accounts url can't be generated using next link");
      }
    } else {
      accountsUrl = getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
          .addPathSegment(GET_ACCOUNTS_SUB_PATH)
          .addQueryParameter(LIMIT_KEY, String.valueOf(pageSize))
          .addQueryParameter(SORT_KEY, SORT_ACCOUNTS_FIELD_NAME)
          .addQueryParameter(USE_CACHE_KEY, String.valueOf(Boolean.FALSE)).build();
      if (filters != null && !filters.isEmpty()) {
        String keyName = filters.entrySet().iterator().next().getKey();
        accountsUrl =
            accountsUrl.newBuilder()
                .addQueryParameter(keyName, SAFE_NAME_FILTER_PREFIX + filters.get(keyName)).build();
      }
    }
    return accountsUrl;
  }

  @NonNull
  private HttpUrl getAccountUrl(String accountId) throws CyberArkPrivilegeCloudException {
    return getSubDomainEndpointsBaseUrl().addPathSegment(API_SUB_PATH)
        .addPathSegment(GET_ACCOUNTS_SUB_PATH).addPathSegment(accountId).build();
  }

  /**
   * This method will return directory services url.
   *
   * @return Directory services url
   */
  public HttpUrl getDirectoryServicesUrl() {
    HttpUrl directoryServicesUrl =
        getBaseUrl().addPathSegments(DIRECTORY_SERVICES_ENDPOINT).build();
    log.debug("Directory services url is:{}", directoryServicesUrl.url());
    return directoryServicesUrl;
  }

  /**
   * This method will return request body for get directory services endpoint. As get directory
   * services is a not paginated endpoint, so payload has not been added as part of request body.
   *
   * @return Request Body with no payload
   */
  public RequestBody getDirectoryServicesRequestBody() {
    return RequestBody.create(new byte[0], JSON_MEDIA_TYPE);
  }

  /**
   * Method to get role rights endpoint's payload.
   *
   * @param roleId Role id for whom rights to be fetched
   * @return RequestBody Created payload
   */
  public RequestBody getRoleRightsPayload(String roleId) {
    String jsonString = String.format("{\n"
        + "    \"role\": \"%s\"\n"
        + "}", roleId);
    log.debug("get role rights payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * Method to get role rights endpoint's url.
   *
   * @return HttpUrl Created url
   */
  public HttpUrl getRoleRightsUrl() {
    HttpUrl roleRightsUrl =
        getBaseUrl().addPathSegments(ROLE_RIGHTS_ENDPOINT).build();
    log.debug("get role rights url is:{}", roleRightsUrl.url());
    return roleRightsUrl;
  }

}
