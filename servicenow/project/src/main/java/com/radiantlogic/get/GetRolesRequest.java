package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.SERVICE_NOW_RESPONSE_GSON;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.ChildRoleRow;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.RoleRow;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to get roles data.
 */
@Slf4j
public class GetRolesRequest {

  /**
   * Used to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Used to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Fetcher for child roles.
   */
  private final RelatedItemsFetcher<RoleRow, ChildRoleRow> childRolesFetcher;

  /**
   * Initializes instance members.
   *
   * @param requestBuilder  Parameter used to get request object for various objects & also
   *                        credentials checker
   * @param requestExecutor Parameter used to execute a request
   * @param batchSize       Parameter used to define parent roles batch size for getting child
   *                        roles
   */
  public GetRolesRequest(final RequestBuilder requestBuilder,
                         final RequestExecutor requestExecutor, final int batchSize) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
    this.childRolesFetcher = RelatedItemsFetcher.<RoleRow, ChildRoleRow>builder()
        .requestBuilder(requestBuilder)
        .requestExecutor(requestExecutor)
        .batchSize(batchSize)
        .parentNameGetter(RoleRow::getName)
        .linkParentNameGetter(ChildRoleRow::getParentRoleName)
        .linkChildNameGetter(ChildRoleRow::getChildRoleName)
        .parentSetter(RoleRow::setChildRoles)
        .requestBuilderFunction(RequestBuilder::getChildRolesRequest)
        .linkResponseType(new TypeToken<ServiceNowResponse<ChildRoleRow>>() {
        }.getType())
        .entityTypeName("child roles")
        .normalizeParentNames(true)
        .initializeEmptyLists(true)
        .build();
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
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getRolesRequest(nextLink, roleName));
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      ServiceNowResponse<RoleRow> serviceNowResponse =
          SERVICE_NOW_RESPONSE_GSON.fromJson(apiResponse,
              new TypeToken<ServiceNowResponse<RoleRow>>() {
              }.getType());
      serviceNowResponse.setNextLink(Util.getNextLink(responseData.getHeaders()).orElse(""));
      checkAndFetchChildRoles(isGetChildRoles, serviceNowResponse);
      return serviceNowResponse;
    }
    throw new ServiceNowException(
        String.format("get role(s) api response message is: %s", apiResponse));
  }

  private void checkAndFetchChildRoles(final boolean isGetChildRoles,
                                       final ServiceNowResponse<RoleRow> serviceNowResponse)
      throws IOException, ServiceNowException {
    if (isGetChildRoles) {
      childRolesFetcher.populateRelatedItems(serviceNowResponse.getResult());
    }
  }
}
