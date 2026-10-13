package com.radiantlogic.http;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.CHILD_ROLES_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.FALSE_QUERY_PARAMETER_VALUE;
import static com.radiantlogic.utility.Constants.FILTER_QUERY_PARAMETER;
import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.INHERITED_QUERY_PARAMETER_KEY;
import static com.radiantlogic.utility.Constants.JSON_MEDIA_TYPE;
import static com.radiantlogic.utility.Constants.SYS_ID_FIELD;
import static com.radiantlogic.utility.Constants.SYS_PARAM_FIELDS_KEY;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.model.AddChildRoleRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Builds HTTP requests for role-child role relationship operations.
 *
 * <p>This class handles the construction of API requests for managing parent-child role
 * relationships in ServiceNow. It delegates to {@link RequestBuilder} for common functionality such
 * as base URL construction, token validation, and caret escaping.
 *
 * <p><b>Design Rationale:</b> This class was extracted to maintain separation of concerns and
 * reduce complexity in RequestBuilder. By isolating role-child relationship request building, we
 * adhere to the Single Responsibility Principle and make the codebase more maintainable.
 */
@Slf4j
@AllArgsConstructor
public class RoleChildRequestBuilder {

  /**
   * Request builder for accessing base URL, token validation, and caret escaping.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Builds an HTTP request to add a child role to a parent role.
   *
   * @param parentRoleName Parent role name to which the child role will be added
   * @param childRoleName  Child role name to be added to the parent role
   * @return Configured HTTP POST request for adding child role to parent role
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getAddChildRoleRequest(final String parentRoleName, final String childRoleName)
      throws ServiceNowException {
    requestBuilder.checkWhetherValidTokenExist();
    return new Request.Builder().url(getAddChildRoleUrl())
        .post(getAddChildRolePayload(parentRoleName, childRoleName))
        .header(AUTHORIZATION_KEYWORD, requestBuilder.getBearerToken().toString()).build();
  }

  private HttpUrl getAddChildRoleUrl() {
    HttpUrl addChildRoleUrl =
        requestBuilder.getBaseUrl()
            .addPathSegments(CHILD_ROLES_ENDPOINT_SUB_PATH)
            .addQueryParameter(SYS_PARAM_FIELDS_KEY, SYS_ID_FIELD)
            .build();
    log.debug("add child role url is: {}", addChildRoleUrl);
    return addChildRoleUrl;
  }

  private RequestBody getAddChildRolePayload(final String parentRoleName,
                                             final String childRoleName) {
    AddChildRoleRequest request =
        new AddChildRoleRequest(parentRoleName, childRoleName);
    String jsonString = GSON.toJson(request);
    log.debug("add child role payload is:{}", jsonString);
    return RequestBody.create(jsonString, JSON_MEDIA_TYPE);
  }

  /**
   * Builds an HTTP request to get role-child role membership sys_id.
   *
   * @param parentRoleName Parent role name
   * @param childRoleName  Child role name
   * @return Configured HTTP GET request for querying membership
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getRoleChildMembershipRequest(final String parentRoleName,
                                               final String childRoleName)
      throws ServiceNowException {
    requestBuilder.checkWhetherValidTokenExist();
    return new Request.Builder().url(getRoleChildMembershipUrl(parentRoleName, childRoleName))
        .get()
        .header(AUTHORIZATION_KEYWORD, requestBuilder.getBearerToken().toString()).build();
  }

  private HttpUrl getRoleChildMembershipUrl(final String parentRoleName,
                                            final String childRoleName) {
    HttpUrl membershipUrl =
        requestBuilder.getBaseUrl()
            .addPathSegments(CHILD_ROLES_ENDPOINT_SUB_PATH)
            .addQueryParameter(FILTER_QUERY_PARAMETER,
                "role.name=" + requestBuilder.escapeCaret(parentRoleName)
                    + "^contains.name=" + requestBuilder.escapeCaret(childRoleName))
            .addQueryParameter(SYS_PARAM_FIELDS_KEY, SYS_ID_FIELD)
            .addQueryParameter(INHERITED_QUERY_PARAMETER_KEY, FALSE_QUERY_PARAMETER_VALUE)
            .build();
    log.debug("get role child membership url is: {}", membershipUrl);
    return membershipUrl;
  }

  /**
   * Builds an HTTP request to delete role-child role membership.
   *
   * @param membershipId System ID of the membership to delete
   * @return Configured HTTP DELETE request
   * @throws ServiceNowException if a valid authentication token is not available
   */
  public Request getDeleteRoleChildMembershipRequest(final String membershipId)
      throws ServiceNowException {
    requestBuilder.checkWhetherValidTokenExist();
    return new Request.Builder().url(getDeleteRoleChildMembershipUrl(membershipId)).delete()
        .header(AUTHORIZATION_KEYWORD, requestBuilder.getBearerToken().toString()).build();
  }

  private HttpUrl getDeleteRoleChildMembershipUrl(final String membershipId) {
    HttpUrl deleteUrl =
        requestBuilder.getBaseUrl()
            .addPathSegments(CHILD_ROLES_ENDPOINT_SUB_PATH)
            .addPathSegment(membershipId)
            .build();
    log.debug("delete role child membership url is: {}", deleteUrl);
    return deleteUrl;
  }
}
