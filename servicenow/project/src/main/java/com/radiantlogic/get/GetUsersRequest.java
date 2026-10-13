package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to get users data.
 */
@Slf4j
public class GetUsersRequest {

  /**
   * Field to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Field to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Field to fetch and populate user roles.
   */
  private final UserRolesFetcher userRolesFetcher;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  Field to get request object for various objects & also credentials
   *                        checker
   * @param requestExecutor Field to execute a request
   * @param batchSize       Parameter used to define batch size for getting batched data
   */
  public GetUsersRequest(final RequestBuilder requestBuilder,
                         final RequestExecutor requestExecutor, final int batchSize) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
    this.userRolesFetcher = new UserRolesFetcher(requestBuilder, requestExecutor, batchSize);
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
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getUsersRequest(nextLink, username));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      ServiceNowResponse<UserRow> serviceNowResponse =
          SERVICE_NOW_RESPONSE_GSON.fromJson(apiResponse,
              new TypeToken<ServiceNowResponse<UserRow>>() {
              }.getType());
      serviceNowResponse.setNextLink(Util.getNextLink(responseData.getHeaders()).orElse(""));
      checkAndFetchUserRoles(isGetUserRoles, serviceNowResponse);
      return serviceNowResponse;
    }
    throw new ServiceNowException(
        String.format("get users api response message is: %s", apiResponse));
  }

  private void checkAndFetchUserRoles(final boolean isGetUserRoles,
                                      final ServiceNowResponse<UserRow> serviceNowResponse)
      throws IOException, ServiceNowException {
    if (isGetUserRoles) {
      userRolesFetcher.populateUserRoles(serviceNowResponse.getResult());
    }
  }
}
