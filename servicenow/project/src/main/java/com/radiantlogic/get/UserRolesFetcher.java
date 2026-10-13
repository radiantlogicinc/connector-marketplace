package com.radiantlogic.get;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRoleRow;
import com.radiantlogic.model.UserRow;
import java.io.IOException;
import java.util.List;

/**
 * Fetches and assigns roles for users.
 */
public class UserRolesFetcher {

  /**
   * Generic fetcher for user roles.
   */
  private final RelatedItemsFetcher<UserRow, UserRoleRow> relatedItemsFetcher;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  parameter to get request object for various objects & also credentials
   *                        checker
   * @param requestExecutor parameter to execute a request
   * @param batchSize       parameter defining fetching user roles for how many users in a go
   */
  public UserRolesFetcher(final RequestBuilder requestBuilder,
                          final RequestExecutor requestExecutor,
                          final int batchSize) {
    this.relatedItemsFetcher = RelatedItemsFetcher.<UserRow, UserRoleRow>builder()
        .requestBuilder(requestBuilder)
        .requestExecutor(requestExecutor)
        .batchSize(batchSize)
        .parentNameGetter(UserRow::getUsername)
        .linkParentNameGetter(UserRoleRow::getUserName)
        .linkChildNameGetter(UserRoleRow::getRoleName)
        .parentSetter(UserRow::setUserRoles)
        .requestBuilderFunction(RequestBuilder::getUserRolesRequest)
        .linkResponseType(new TypeToken<ServiceNowResponse<UserRoleRow>>() {
        }.getType())
        .entityTypeName("user roles")
        .normalizeParentNames(true)
        .initializeEmptyLists(true)
        .build();
  }

  /**
   * Method to fetch and assign roles for users.
   *
   * @param users List of users whose roles are to be fetched
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case api response is not ok/In case link header is malformed
   */
  public void populateUserRoles(final List<UserRow> users)
      throws IOException, ServiceNowException {
    relatedItemsFetcher.populateRelatedItems(users);
  }
}
