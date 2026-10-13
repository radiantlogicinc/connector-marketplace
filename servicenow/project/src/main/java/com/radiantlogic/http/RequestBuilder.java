package com.radiantlogic.http;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.BASE_URL_PREFIX;
import static com.radiantlogic.utility.Constants.BATCH_SIZE_KEY;
import static com.radiantlogic.utility.Constants.BEARER_KEYWORD_WITH_SPACE;
import static com.radiantlogic.utility.Constants.CHILD_ROLES_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.CHILD_ROLES_FILTER_FIELD;
import static com.radiantlogic.utility.Constants.CHILD_ROLES_SYSPARM_FIELDS;
import static com.radiantlogic.utility.Constants.CLIENT_CREDENTIALS_GRANT_TYPE;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_SECRET_KEY;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.FALSE_QUERY_PARAMETER_VALUE;
import static com.radiantlogic.utility.Constants.FILTER_QUERY_PARAMETER;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.GRANT_TYPE_KEY;
import static com.radiantlogic.utility.Constants.GROUPS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.GROUP_ROLES_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.GROUP_ROLES_FILTER_FIELD;
import static com.radiantlogic.utility.Constants.GROUP_ROLES_SYSPARM_FIELDS;
import static com.radiantlogic.utility.Constants.GROUP_USERS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.GROUP_USERS_FILTER_FIELD;
import static com.radiantlogic.utility.Constants.GROUP_USERS_SYSPARM_FIELDS;
import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.INHERITED_QUERY_PARAMETER_KEY;
import static com.radiantlogic.utility.Constants.INSTANCE_NAME_KEY;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX;
import static com.radiantlogic.utility.Constants.OR_SEPARATOR;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_KEY;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_QUERY_PARAMETER;
import static com.radiantlogic.utility.Constants.ROLES_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.SYS_ID_FIELD;
import static com.radiantlogic.utility.Constants.SYS_PARAM_FIELDS_KEY;
import static com.radiantlogic.utility.Constants.TOKEN_EXPIRY_BUFFER_SECONDS;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_SERVICENOW_MESSAGE;
import static com.radiantlogic.utility.Constants.URL_HOST_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.URL_SCHEME;
import static com.radiantlogic.utility.Constants.URL_SCHEME_KEY;
import static com.radiantlogic.utility.Constants.USERS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.USER_ROLES_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.USER_ROLES_FILTER_FIELD;
import static com.radiantlogic.utility.Constants.USER_ROLES_SYSPARM_FIELDS;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.model.AccessToken;
import com.radiantlogic.model.AddRoleToUserRequest;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.UserRow;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to provide request objects for various CRUD operations with credential validator.
 *
 * <p><b>Thread safety:</b> This class is not thread-safe. It maintains a mutable bearer token
 * cache that is updated without synchronization. In the current connector runtime model this is
 * safe because each connector instance executes within its own class loader and is not shared
 * across threads. If the execution model changes in the future, external synchronization or a
 * thread-safe token cache may be required.
 */
@Slf4j
@Getter
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.GodClass"})
public final class RequestBuilder {

  /**
   * Field to store connector configuration parameter - instance name.
   */
  private final String instanceName;
  /**
   * Field to store connector configuration parameter - client id.
   */
  @Getter(AccessLevel.NONE)
  private final String clientId;
  /**
   * Field to store connector configuration parameter - client secret.
   */
  @Getter(AccessLevel.NONE)
  private final String clientSecret;
  /**
   * Field to store connector configuration parameter - page size.
   */
  private final int pageSize;
  /**
   * Field to store an optional url scheme.
   */
  private final String scheme;
  /**
   * Field to store an optional url host.
   */
  private final String host;
  /**
   * Field to store an optional url port.
   */
  private final Long port;
  /**
   * Field used to execute requests.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Field used to store access token & its expiry.
   */
  @Getter(AccessLevel.PACKAGE)
  private BearerToken bearerToken = new BearerToken(StringUtils.EMPTY, Instant.ofEpochSecond(0L));
  /**
   * Constant to define url max length.
   */
  private static final int MAX_URL_LENGTH = 2048;
  /**
   * Field to store connector configuration parameter - batch size.
   */
  private final int batchSize;

  /**
   * Constructor to initialize instance members.
   *
   * @param connectionProperties Contains values to be used for members initialization
   */
  public RequestBuilder(
      @Property(name = CUSTOM_DATASOURCE_PROPERTIES) final ReadOnlyProperties connectionProperties,
      final RequestExecutor requestExecutor) {
    this.requestExecutor = requestExecutor;
    instanceName = Objects.requireNonNull(connectionProperties.get(INSTANCE_NAME_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, INSTANCE_NAME_KEY))
        .toString();
    clientId = Objects.requireNonNull(connectionProperties.get(CLIENT_ID_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, CLIENT_ID_KEY)).toString();
    clientSecret = Objects.requireNonNull(connectionProperties.get(CLIENT_SECRET_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, CLIENT_SECRET_KEY)).toString();
    pageSize = Integer.parseInt(Objects.requireNonNull(connectionProperties.get(PAGE_SIZE_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, PAGE_SIZE_KEY)).toString());
    scheme = Optional.ofNullable(connectionProperties.get(URL_SCHEME_KEY))
        .map(Object::toString)
        .orElse("");
    host = Optional.ofNullable(connectionProperties.get(URL_HOST_KEY))
        .map(Object::toString)
        .orElse("");

    port = Optional.ofNullable(connectionProperties.get(URL_PORT_KEY))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
    batchSize = Integer.parseInt(Objects.requireNonNull(connectionProperties.get(BATCH_SIZE_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, BATCH_SIZE_KEY)).toString());
  }

  @VisibleForTesting
  Request getTokenRequest() {
    return new Request.Builder().url(getAccessTokenUrl()).post(getFormBodyForAccessToken()).build();
  }

  private HttpUrl getAccessTokenUrl() {
    HttpUrl tokenUrl = getBaseUrl().addPathSegments(GENERATE_ACCESS_TOKEN_ENDPOINT).build();
    log.debug("url to generate access token is: {}", tokenUrl.url());
    return tokenUrl;
  }

  /**
   * Method to get base url.
   *
   * @return HttpUrl.Builder containing base url
   */
  public HttpUrl.Builder getBaseUrl() {
    HttpUrl.Builder builder =
        new HttpUrl.Builder().scheme(StringUtils.isNotBlank(scheme) ? scheme : URL_SCHEME)
            .host(
                StringUtils.isNotBlank(host) ? host : String.format(BASE_URL_PREFIX, instanceName));
    if (port != null && port > 0) {
      builder.port(port.intValue());
    }
    return builder;
  }

  private FormBody getFormBodyForAccessToken() {
    return new FormBody.Builder().add(GRANT_TYPE_KEY, CLIENT_CREDENTIALS_GRANT_TYPE)
        .add(CLIENT_ID_KEY, clientId).add(CLIENT_SECRET_KEY, clientSecret).build();
  }

  /**
   * Method to check whether valid token is there, if not, a new will be generated & stored.
   *
   * @return boolean true or false
   */
  public boolean canConnectWithValidToken() {
    try {
      if (bearerToken.isExpired()) {
        generateToken();
      }
      return !bearerToken.isExpired();
    } catch (JsonSyntaxException e) {
      log.error(
          "Invalid response received, please check whether instance is not in hibernate state", e);
      return false;
    } catch (ServiceNowException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
      return false;
    }
  }

  private void generateToken() throws ServiceNowException, IOException {
    ResponseData responseData = requestExecutor.execute(getTokenRequest());
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      AccessToken accessToken = GSON.fromJson(apiResponse, AccessToken.class);
      if (StringUtils.isNotBlank(accessToken.getToken())) {
        bearerToken = new BearerToken(BEARER_KEYWORD_WITH_SPACE.concat(accessToken.getToken()),
            Instant.now().plusSeconds(accessToken.getExpiresIn()));
        log.info("generated new token");
        return;
      }
    }
    throw new ServiceNowException(
        String.format("Unable to generate access token: %s", apiResponse));
  }

  /**
   * Class to store access token & its expiry. It is intentionally package-private for testing
   */
  @AllArgsConstructor
  static class BearerToken {

    /**
     * Field to store access token.
     */
    final String token;
    /**
     * Field to store token expiry.
     */
    final Instant expiration;

    boolean isExpired() {
      // as part of defensive coding, reducing token expiry by 2 min while calculating its expiry
      return expiration.isBefore(Instant.now().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
    }

    @Override
    public String toString() {
      return token;
    }
  }

  private HttpUrl getUsersUrl(final String nextLink, final String username)
      throws ServiceNowException {
    HttpUrl usersUrl;
    if (StringUtils.isBlank(username)) {
      if (StringUtils.isNotBlank(nextLink)) {
        usersUrl = HttpUrl.parse(nextLink);
        if (usersUrl == null) {
          throw new ServiceNowException("Invalid nextLink URL");
        }
        checkWhetherNextLinkBaseUrlIsCorrect(usersUrl);
      } else {
        usersUrl =
            getBaseUrl()
                .addPathSegments(USERS_ENDPOINT_SUB_PATH)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAMETER, String.valueOf(pageSize))
                .build();
      }
    } else {
      usersUrl =
          getBaseUrl()
              .addPathSegments(USERS_ENDPOINT_SUB_PATH)
              .addQueryParameter(
                  FILTER_QUERY_PARAMETER,
                  "user_name=" + escapeCaret(username)
              )
              .build();
    }
    log.debug("get user(s) url is: {}", usersUrl);
    return usersUrl;
  }

  /**
   * Escapes values used in ServiceNow sysparm_query parameters.
   *
   * @param value the raw field value to escape
   * @return the escaped value with carets doubled
   */
  String escapeCaret(final String value) {

    /*
    Design Rationale

   * <p><b>Caret doubling:</b> The caret ('^') is ServiceNow's encoded-query field separator
   * (e.g., "field1=value1^field2=value2" or "name=value1^ORname=value2"). When a literal caret
   * appears in a field value, it must be doubled ("^^") to prevent ServiceNow from misinterpreting
   * it as a separator. This is a ServiceNow-specific escape mechanism, not standard URL encoding.
   *
   * <p><b>URL encoding:</b> OkHttp's {@code addQueryParameter()} automatically percent-encodes
   * special characters (spaces, ampersands, etc.) for safe transmission in URLs. Therefore, this
   * method does NOT perform URL encoding—only ServiceNow-specific escaping.
   *
   * <p><b>Commas are NOT escaped:</b> Unlike carets, commas do NOT have special meaning in
   * ServiceNow's sysparm_query syntax. ServiceNow treats commas as literal characters within
   * field values, so they require no escaping. This behavior is validated by integration tests
   * such as {@code testGetRoleWithCommaInNameVerifyChildRolesField}.
     */
    return value.replace("^", "^^");
  }

  /**
   * Method that verifies that the host of a pagination nextLink URL matches the configured
   * ServiceNow instance base URL.
   *
   * <p>This defensive check prevents following pagination links that point to a
   * different or malicious host, ensuring all subsequent requests remain scoped to the expected
   * ServiceNow instance.
   *
   * @param url nextLink URL returned by ServiceNow
   * @throws ServiceNowException if the nextLink host does not match the configured instance host
   */
  private void checkWhetherNextLinkBaseUrlIsCorrect(final HttpUrl url)
      throws ServiceNowException {
    String host = url.host();
    String expectedHost = getBaseUrl().build().host();
    if (!host.equals(expectedHost)) {
      throw new ServiceNowException(
          String.format("nextLink host does not match instance. Expected: %s but got: %s",
              expectedHost, host));
    }
  }

  private HttpUrl getGroupsUrl(final String nextLink, final String groupName)
      throws ServiceNowException {
    HttpUrl groupsUrl;
    if (StringUtils.isBlank(groupName)) {
      if (StringUtils.isNotBlank(nextLink)) {
        groupsUrl = HttpUrl.parse(nextLink);
        if (groupsUrl == null) {
          throw new ServiceNowException("Invalid nextLink URL to get groups");
        }
        checkWhetherNextLinkBaseUrlIsCorrect(groupsUrl);
      } else {
        groupsUrl =
            getBaseUrl()
                .addPathSegments(GROUPS_ENDPOINT_SUB_PATH)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAMETER, String.valueOf(pageSize))
                .build();
      }
    } else {
      groupsUrl =
          getBaseUrl()
              .addPathSegments(GROUPS_ENDPOINT_SUB_PATH)
              .addQueryParameter(
                  FILTER_QUERY_PARAMETER,
                  "name=" + escapeCaret(groupName)
              )
              .build();
    }
    log.debug("get group(s) url is: {}", groupsUrl);
    return groupsUrl;
  }

  /**
   * Method to get user(s) request object.
   *
   * @param nextLink Url to get next set of paginated data
   * @param username Username to get a particular user record
   * @return Request object
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getUsersRequest(final String nextLink, final String username)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getUsersUrl(nextLink, username)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  /**
   * Returns group(s) request object.
   *
   * @param nextLink  Url to get next set of paginated data
   * @param groupName Group name to get a particular group record
   * @return Request object
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getGroupsRequest(final String nextLink, final String groupName)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getGroupsUrl(nextLink, groupName)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getRolesUrl(final String nextLink, final String roleName)
      throws ServiceNowException {
    HttpUrl rolesUrl;
    if (StringUtils.isBlank(roleName)) {
      if (StringUtils.isNotBlank(nextLink)) {
        rolesUrl = HttpUrl.parse(nextLink);
        if (rolesUrl == null) {
          throw new ServiceNowException("Invalid nextLink URL to get roles");
        }
        checkWhetherNextLinkBaseUrlIsCorrect(rolesUrl);
      } else {
        rolesUrl =
            getBaseUrl()
                .addPathSegments(ROLES_ENDPOINT_SUB_PATH)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAMETER, String.valueOf(pageSize))
                .build();
      }
    } else {
      rolesUrl =
          getBaseUrl()
              .addPathSegments(ROLES_ENDPOINT_SUB_PATH)
              .addQueryParameter(
                  FILTER_QUERY_PARAMETER,
                  "name=" + escapeCaret(roleName))
              .build();
    }
    log.debug("get role(s) url is: {}", rolesUrl);
    return rolesUrl;
  }

  /**
   * Returns role(s) request object.
   *
   * @param nextLink Url to get next set of paginated data
   * @param roleName Role name to get a particular role record
   * @return Request object
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getRolesRequest(final String nextLink, final String roleName)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getRolesUrl(nextLink, roleName)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getChildRolesUrl(final String nextLink, final List<String> roleNames)
      throws ServiceNowException {
    return buildFilteredListUrl(nextLink, roleNames, CHILD_ROLES_ENDPOINT_SUB_PATH,
        CHILD_ROLES_FILTER_FIELD, CHILD_ROLES_SYSPARM_FIELDS, "child roles");
  }

  private void checkUrlLength(final HttpUrl url)
      throws ServiceNowException {
    if (url.toString().length() > MAX_URL_LENGTH) {
      throw new ServiceNowException(
          String.format("URL length %d exceeds max supported length %d", url.toString().length(),
              MAX_URL_LENGTH));
    }
  }

  /**
   * Returns child roles request object for given parent roles.
   *
   * @param nextLink  Url to get next set of paginated child roles data
   * @param roleNames List of parent role names to get child roles
   * @return Request Get Child Roles request
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getChildRolesRequest(final String nextLink, final List<String> roleNames)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getChildRolesUrl(nextLink, roleNames)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getGroupUsersUrl(final String nextLink, final List<String> groupNames)
      throws ServiceNowException {
    return buildFilteredListUrl(nextLink, groupNames, GROUP_USERS_ENDPOINT_SUB_PATH,
        GROUP_USERS_FILTER_FIELD, GROUP_USERS_SYSPARM_FIELDS, "group users");
  }

  /**
   * Returns group users request object for given groups.
   *
   * @param nextLink   Url to get next set of paginated group users data
   * @param groupNames List of group names to get users
   * @return Request Get Group Users request
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getGroupUsersRequest(final String nextLink, final List<String> groupNames)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getGroupUsersUrl(nextLink, groupNames)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getGroupRolesUrl(final String nextLink, final List<String> groupNames)
      throws ServiceNowException {
    return buildFilteredListUrl(nextLink, groupNames, GROUP_ROLES_ENDPOINT_SUB_PATH,
        GROUP_ROLES_FILTER_FIELD, GROUP_ROLES_SYSPARM_FIELDS, "group roles");
  }

  /**
   * Returns group roles request object for given groups.
   *
   * @param nextLink   Url to get next set of paginated group roles data
   * @param groupNames List of group names to get roles
   * @return Request Get Group Roles request
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getGroupRolesRequest(final String nextLink, final List<String> groupNames)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getGroupRolesUrl(nextLink, groupNames)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getUserRolesUrl(final String nextLink, final List<String> userNames)
      throws ServiceNowException {
    return buildFilteredListUrl(nextLink, userNames, USER_ROLES_ENDPOINT_SUB_PATH,
        USER_ROLES_FILTER_FIELD, USER_ROLES_SYSPARM_FIELDS, "user roles");
  }

  /**
   * Builds a URL for fetching related items with OR-joined filter query.
   *
   * @param nextLink        Next link for pagination (empty for first request)
   * @param names           List of parent entity names to filter by
   * @param endpointSubPath API endpoint sub-path
   * @param filterField     Filter field prefix (e.g., "role.name=")
   * @param sysParmFields   Sys-parm fields value for the response
   * @param errorLabel      Label for error messages (e.g., "child roles")
   * @return Built HttpUrl
   * @throws ServiceNowException If nextLink is invalid or URL is too long
   */
  private HttpUrl buildFilteredListUrl(final String nextLink, final List<String> names,
                                       final String endpointSubPath, final String filterField,
                                       final String sysParmFields, final String errorLabel)
      throws ServiceNowException {
    HttpUrl url;
    if (StringUtils.isNotBlank(nextLink)) {
      url = HttpUrl.parse(nextLink);
      if (url == null) {
        throw new ServiceNowException(
            String.format("Invalid nextLink URL to get %s", errorLabel));
      }
      checkWhetherNextLinkBaseUrlIsCorrect(url);
    } else {
      List<String> escapedNames = new ArrayList<>();
      for (String name : names) {
        escapedNames.add(escapeCaret(name));
      }
      String namesQuery =
          escapedNames.stream()
              .map(name -> filterField + name)
              .collect(Collectors.joining(OR_SEPARATOR));
      url =
          getBaseUrl()
              .addPathSegments(endpointSubPath)
              .addQueryParameter(FILTER_QUERY_PARAMETER, namesQuery)
              .addQueryParameter(SYS_PARAM_FIELDS_KEY, sysParmFields)
              .addQueryParameter(PAGE_SIZE_QUERY_PARAMETER, String.valueOf(pageSize))
              .addQueryParameter(INHERITED_QUERY_PARAMETER_KEY, FALSE_QUERY_PARAMETER_VALUE)
              .build();
      checkUrlLength(url);
    }
    log.debug("get {} url is: {}", errorLabel, url);
    return url;
  }

  /**
   * Returns user(s) roles request object for given users.
   *
   * @param nextLink  Url to get next set of paginated user roles data
   * @param userNames List of user names to get roles
   * @return Request Get User Roles request
   * @throws ServiceNowException In case valid token/credentials not available/invalid next link
   *                             received or next link base url is not correct
   */
  public Request getUserRolesRequest(final String nextLink, final List<String> userNames)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getUserRolesUrl(nextLink, userNames)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  void checkWhetherValidTokenExist() throws ServiceNowException {
    if (!canConnectWithValidToken()) {
      throw new ServiceNowException(UNABLE_TO_CONNECT_WITH_SERVICENOW_MESSAGE);
    }
  }

  @VisibleForTesting
  void setBearerToken(final BearerToken bearerToken) {
    this.bearerToken = bearerToken;
  }

  private HttpUrl getDeleteUserUrl(final String sysId) {
    HttpUrl deleteUserUrl =
        getBaseUrl()
            .addPathSegments(USERS_ENDPOINT_SUB_PATH)
            .addPathSegment(sysId)
            .build();
    log.debug("delete user url is: {}", deleteUserUrl);
    return deleteUserUrl;
  }

  /**
   * Method to generate delete user request.
   *
   * @param sysId Sys id of user to be deleted
   * @return Request Delete user request object
   * @throws ServiceNowException In case of valid token/credentials not found
   */
  public Request getDeleteUserRequest(final String sysId) throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getDeleteUserUrl(sysId)).delete()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getUpdateUserUrl(final String sysId) {
    HttpUrl updateUserUrl =
        getBaseUrl()
            .addPathSegments(USERS_ENDPOINT_SUB_PATH)
            .addPathSegment(sysId)
            .build();
    log.debug("update user url is: {}", updateUserUrl);
    return updateUserUrl;
  }

  private RequestBody getUpdateUserPayload(final UserRow userRow) {
    String jsonString = GSON.toJson(userRow);
    log.debug("update user payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * Generates update user request.
   *
   * @param userRow Payload containing values to be updated
   * @param sysId   Sys id of the user to be updated
   * @return Request object
   * @throws ServiceNowException In case valid token cannot be collected or the sys id received is
   *                             malformed
   */
  public Request getUpdateUserRequest(final UserRow userRow, final String sysId)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getUpdateUserUrl(sysId)).patch(getUpdateUserPayload(userRow))
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getCreateUserUrl() {
    HttpUrl createUserUrl =
        getBaseUrl().addPathSegments(USERS_ENDPOINT_SUB_PATH).build();
    log.debug("create user url is: {}", createUserUrl);
    return createUserUrl;
  }

  private RequestBody getCreateUserPayload(final UserRow userRow) {
    String jsonString = GSON.toJson(userRow);
    log.debug("create user payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * Builds an HTTP request to create a new user.
   *
   * @param userRow User data to be sent in the request payload
   * @return Configured HTTP POST request for user creation
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getCreateUserRequest(final UserRow userRow)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getCreateUserUrl()).post(getCreateUserPayload(userRow))
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  /**
   * Builds an HTTP request to add a role to a user.
   *
   * @param roleName Role name to be assigned to the user
   * @param userName User name to whom the role will be assigned
   * @return Configured HTTP POST request for adding role to user
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getAddRoleToUserRequest(final String roleName, final String userName)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getAddRoleToUserUrl())
        .post(getAddRoleToUserPayload(roleName, userName))
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getAddRoleToUserUrl() {
    HttpUrl addRoleToUserUrl =
        getBaseUrl()
            .addPathSegments(USER_ROLES_ENDPOINT_SUB_PATH)
            .addQueryParameter(SYS_PARAM_FIELDS_KEY, SYS_ID_FIELD)
            .build();
    log.debug("add role to user url is: {}", addRoleToUserUrl);
    return addRoleToUserUrl;
  }

  private RequestBody getAddRoleToUserPayload(final String roleName, final String userName) {

    // escape caret not req here as part of the payload not url
    AddRoleToUserRequest request =
        new AddRoleToUserRequest(userName, roleName);
    String jsonString = GSON.toJson(request);
    log.debug("add role to user payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * Builds an HTTP request to get user-role membership sys_id.
   *
   * @param userName User name
   * @param roleName Role name
   * @return Configured HTTP GET request for querying membership
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getUserRoleMembershipRequest(final String userName, final String roleName)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getUserRoleMembershipUrl(userName, roleName)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getUserRoleMembershipUrl(final String userName, final String roleName) {
    HttpUrl membershipUrl =
        getBaseUrl()
            .addPathSegments(USER_ROLES_ENDPOINT_SUB_PATH)
            .addQueryParameter(FILTER_QUERY_PARAMETER,
                "user.user_name=" + escapeCaret(userName) + "^role.name=" + escapeCaret(roleName))
            .addQueryParameter(SYS_PARAM_FIELDS_KEY, SYS_ID_FIELD)
            .addQueryParameter(INHERITED_QUERY_PARAMETER_KEY, FALSE_QUERY_PARAMETER_VALUE)
            .build();
    log.debug("get user role membership url is: {}", membershipUrl);
    return membershipUrl;
  }

  /**
   * Builds an HTTP request to delete user-role membership.
   *
   * @param membershipId System ID of the membership to delete
   * @return Configured HTTP DELETE request
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getDeleteUserRoleMembershipRequest(final String membershipId)
      throws ServiceNowException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getDeleteUserRoleMembershipUrl(membershipId)).delete()
        .header(AUTHORIZATION_KEYWORD, bearerToken.toString()).build();
  }

  private HttpUrl getDeleteUserRoleMembershipUrl(final String membershipId) {
    HttpUrl deleteUrl =
        getBaseUrl()
            .addPathSegments(USER_ROLES_ENDPOINT_SUB_PATH)
            .addPathSegment(membershipId)
            .build();
    log.debug("delete user role membership url is: {}", deleteUrl);
    return deleteUrl;
  }

}
