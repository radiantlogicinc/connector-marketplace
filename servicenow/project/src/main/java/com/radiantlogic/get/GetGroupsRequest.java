package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.GroupRoleRow;
import com.radiantlogic.model.GroupRow;
import com.radiantlogic.model.GroupUserRow;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to get groups data.
 */
@Slf4j
public class GetGroupsRequest {

  /**
   * Used to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Used to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Fetcher for group users.
   */
  private final RelatedItemsFetcher<GroupRow, GroupUserRow> groupUsersFetcher;
  /**
   * Fetcher for group roles.
   */
  private final RelatedItemsFetcher<GroupRow, GroupRoleRow> groupRolesFetcher;

  /**
   * Initializes instance members.
   *
   * @param requestBuilder  Used to get request object for various objects & also credentials
   *                        checker
   * @param requestExecutor Used to execute a request
   * @param batchSize       Parameter used to define groups batch size for getting related data
   */
  public GetGroupsRequest(final RequestBuilder requestBuilder,
                          final RequestExecutor requestExecutor, final int batchSize) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
    this.groupUsersFetcher = RelatedItemsFetcher.<GroupRow, GroupUserRow>builder()
        .requestBuilder(requestBuilder)
        .requestExecutor(requestExecutor)
        .batchSize(batchSize)
        .parentNameGetter(GroupRow::getName)
        .linkParentNameGetter(GroupUserRow::getGroupName)
        .linkChildNameGetter(GroupUserRow::getUserName)
        .parentSetter(GroupRow::setGroupUsers)
        .requestBuilderFunction(RequestBuilder::getGroupUsersRequest)
        .linkResponseType(new TypeToken<ServiceNowResponse<GroupUserRow>>() {
        }.getType())
        .entityTypeName("group users")
        .normalizeParentNames(false)
        .initializeEmptyLists(true)
        .build();
    this.groupRolesFetcher = RelatedItemsFetcher.<GroupRow, GroupRoleRow>builder()
        .requestBuilder(requestBuilder)
        .requestExecutor(requestExecutor)
        .batchSize(batchSize)
        .parentNameGetter(GroupRow::getName)
        .linkParentNameGetter(GroupRoleRow::getGroupName)
        .linkChildNameGetter(GroupRoleRow::getRoleName)
        .parentSetter(GroupRow::setGroupRoles)
        .requestBuilderFunction(RequestBuilder::getGroupRolesRequest)
        .linkResponseType(new TypeToken<ServiceNowResponse<GroupRoleRow>>() {
        }.getType())
        .entityTypeName("group roles")
        .normalizeParentNames(false)
        .initializeEmptyLists(true)
        .build();
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
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getGroupsRequest(nextLink, groupName));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      ServiceNowResponse<GroupRow> serviceNowResponse =
          SERVICE_NOW_RESPONSE_GSON.fromJson(apiResponse,
              new TypeToken<ServiceNowResponse<GroupRow>>() {
              }.getType());
      serviceNowResponse.setNextLink(Util.getNextLink(responseData.getHeaders()).orElse(""));
      checkAndFetchGroupUsers(isGetGroupUsers, serviceNowResponse);
      checkAndFetchGroupRoles(isGetGroupRoles, serviceNowResponse);
      return serviceNowResponse;
    }
    throw new ServiceNowException(
        String.format("get group(s) api response message is: %s", apiResponse));
  }

  private void checkAndFetchGroupUsers(final boolean isGetGroupUsers,
                                       final ServiceNowResponse<GroupRow> serviceNowResponse)
      throws IOException, ServiceNowException {
    if (isGetGroupUsers) {
      groupUsersFetcher.populateRelatedItems(serviceNowResponse.getResult());
    }
  }

  private void checkAndFetchGroupRoles(final boolean isGetGroupRoles,
                                       final ServiceNowResponse<GroupRow> serviceNowResponse)
      throws IOException, ServiceNowException {
    if (isGetGroupRoles) {
      groupRolesFetcher.populateRelatedItems(serviceNowResponse.getResult());
    }
  }
}
