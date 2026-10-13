package com.radiantlogic.get;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.GroupRow;
import com.radiantlogic.model.RoleRow;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to get data for various objects.
 */
@Slf4j
public class GetRequest {

  /**
   * Field to get users data.
   */
  private final GetUsersRequest getUsersRequest;
  /**
   * Used to get groups data.
   */
  private final GetGroupsRequest getGroupsRequest;
  /**
   * Used to get roles data.
   */
  private final GetRolesRequest getRolesRequest;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  Parameter to get request object for various objects & also credentials
   *                        checker
   * @param requestExecutor Parameter used to execute a request
   * @param batchSize       Parameter used to define batch size for getting various data in batches
   */
  public GetRequest(final RequestBuilder requestBuilder, final RequestExecutor requestExecutor,
                    final int batchSize) {
    this.getUsersRequest = new GetUsersRequest(requestBuilder, requestExecutor, batchSize);
    this.getGroupsRequest = new GetGroupsRequest(requestBuilder, requestExecutor, batchSize);
    this.getRolesRequest = new GetRolesRequest(requestBuilder, requestExecutor, batchSize);
  }

  /**
   * Method to get users. Upon getting any exception while fetching user roles, entire flow will
   * abort as failure.
   *
   * @param nextLink       Url to get next set of paginated data
   * @param username       Username to get a particular record
   * @param isGetUserRoles Whether user roles to be fetched
   * @return ServiceNowResponse containing fetched users and next link
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case api response is not ok/In case link header is malformed
   */
  public ServiceNowResponse<UserRow> getUsers(final String nextLink, final String username,
                                              final boolean isGetUserRoles)
      throws IOException, ServiceNowException {
    return getUsersRequest.getUsers(nextLink, username, isGetUserRoles);
  }

  /**
   * Collects group(s).
   *
   * @param nextLink        Url to get next set of paginated data
   * @param groupName       Group name to get a particular record
   * @param isGetGroupUsers Whether group users to be fetched
   * @param isGetGroupRoles Whether group roles to be fetched
   * @return ServiceNowResponse Contains fetched groups and the next link
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case api response is not ok/In case link header is malformed
   */
  public ServiceNowResponse<GroupRow> getGroups(final String nextLink, final String groupName,
                                                final boolean isGetGroupUsers,
                                                final boolean isGetGroupRoles)
      throws IOException, ServiceNowException {
    return getGroupsRequest.getGroups(nextLink, groupName, isGetGroupUsers, isGetGroupRoles);
  }

  /**
   * Collects role(s).
   *
   * @param nextLink        Url to get next set of paginated data
   * @param roleName        Role name to get a particular record
   * @param isGetChildRoles Whether child roles to be fetched
   * @return ServiceNowResponse Contains fetched roles and the next link
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case api response is not ok/In case link header is malformed
   */
  public ServiceNowResponse<RoleRow> getRoles(final String nextLink, final String roleName,
                                              final boolean isGetChildRoles)
      throws IOException, ServiceNowException {
    return getRolesRequest.getRoles(nextLink, roleName, isGetChildRoles);
  }
}
