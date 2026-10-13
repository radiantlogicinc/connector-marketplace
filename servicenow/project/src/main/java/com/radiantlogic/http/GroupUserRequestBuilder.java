package com.radiantlogic.http;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.FALSE_QUERY_PARAMETER_VALUE;
import static com.radiantlogic.utility.Constants.FILTER_QUERY_PARAMETER;
import static com.radiantlogic.utility.Constants.GROUP_USERS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.INHERITED_QUERY_PARAMETER_KEY;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.SYS_ID_FIELD;
import static com.radiantlogic.utility.Constants.SYS_PARAM_FIELDS_KEY;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.model.AddUserToGroupRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Builds HTTP requests for group-user relationship operations.
 *
 * <p>This class is responsible for constructing requests to manage user memberships in groups
 * through the ServiceNow sys_user_grmember API. It handles URL construction, payload generation,
 * and authentication header injection for add, query, and delete operations.
 *
 * <p><b>Design Rationale:</b> This class was extracted from RequestBuilder to reduce cyclomatic
 * complexity and adhere to the Single Responsibility Principle. By separating group-user
 * relationship concerns, we avoid the God Class anti-pattern and make the codebase more
 * maintainable.
 */
@Slf4j
@AllArgsConstructor
public class GroupUserRequestBuilder {

  /**
   * Request builder for accessing base URL, token validation, and caret escaping.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Builds an HTTP request to add a user to a group.
   *
   * @param groupName Group name to which the user will be added
   * @param userName  User name to be added to the group
   * @return Configured HTTP POST request for adding user to group
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getAddUserToGroupRequest(final String groupName, final String userName)
      throws ServiceNowException {
    requestBuilder.checkWhetherValidTokenExist();
    return new Request.Builder().url(getAddUserToGroupUrl())
        .post(getAddUserToGroupPayload(groupName, userName))
        .header(AUTHORIZATION_KEYWORD, requestBuilder.getBearerToken().toString()).build();
  }

  private HttpUrl getAddUserToGroupUrl() {
    HttpUrl addUserToGroupUrl =
        requestBuilder.getBaseUrl()
            .addPathSegments(GROUP_USERS_ENDPOINT_SUB_PATH)
            .addQueryParameter(SYS_PARAM_FIELDS_KEY, SYS_ID_FIELD)
            .build();
    log.debug("add user to group url is: {}", addUserToGroupUrl);
    return addUserToGroupUrl;
  }

  private RequestBody getAddUserToGroupPayload(final String groupName, final String userName) {
    AddUserToGroupRequest request =
        new AddUserToGroupRequest(userName, groupName);
    String jsonString = GSON.toJson(request);
    log.debug("add user to group payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * Builds an HTTP request to get user-group membership sys_id.
   *
   * @param userName  User name
   * @param groupName Group name
   * @return Configured HTTP GET request for querying membership
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getUserGroupMembershipRequest(final String userName, final String groupName)
      throws ServiceNowException {
    requestBuilder.checkWhetherValidTokenExist();
    return new Request.Builder().url(getUserGroupMembershipUrl(userName, groupName)).get()
        .header(AUTHORIZATION_KEYWORD, requestBuilder.getBearerToken().toString()).build();
  }

  private HttpUrl getUserGroupMembershipUrl(final String userName, final String groupName) {
    HttpUrl membershipUrl =
        requestBuilder.getBaseUrl()
            .addPathSegments(GROUP_USERS_ENDPOINT_SUB_PATH)
            .addQueryParameter(FILTER_QUERY_PARAMETER,
                "user.user_name=" + requestBuilder.escapeCaret(userName)
                    + "^group.name=" + requestBuilder.escapeCaret(groupName))
            .addQueryParameter(SYS_PARAM_FIELDS_KEY, SYS_ID_FIELD)
            .addQueryParameter(INHERITED_QUERY_PARAMETER_KEY, FALSE_QUERY_PARAMETER_VALUE)
            .build();
    log.debug("get user group membership url is: {}", membershipUrl);
    return membershipUrl;
  }

  /**
   * Builds an HTTP request to delete user-group membership.
   *
   * @param membershipId System ID of the membership to delete
   * @return Configured HTTP DELETE request
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getDeleteUserGroupMembershipRequest(final String membershipId)
      throws ServiceNowException {
    requestBuilder.checkWhetherValidTokenExist();
    return new Request.Builder().url(getDeleteUserGroupMembershipUrl(membershipId)).delete()
        .header(AUTHORIZATION_KEYWORD, requestBuilder.getBearerToken().toString()).build();
  }

  private HttpUrl getDeleteUserGroupMembershipUrl(final String membershipId) {
    HttpUrl deleteUrl =
        requestBuilder.getBaseUrl()
            .addPathSegments(GROUP_USERS_ENDPOINT_SUB_PATH)
            .addPathSegment(membershipId)
            .build();
    log.debug("delete user group membership url is: {}", deleteUrl);
    return deleteUrl;
  }
}
