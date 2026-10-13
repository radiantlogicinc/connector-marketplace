package com.radiantlogic.get;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.ChildRoleRow;
import com.radiantlogic.model.RoleRow;
import com.radiantlogic.model.ServiceNowResponse;
import java.io.IOException;
import java.util.List;

/**
 * Fetches and assigns child roles for roles.
 */
public class RoleChildrenFetcher {

  /**
   * Generic fetcher for role children.
   */
  private final RelatedItemsFetcher<RoleRow, ChildRoleRow> relatedItemsFetcher;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  parameter to get request object for various objects & also credentials
   *                        checker
   * @param requestExecutor parameter to execute a request
   * @param batchSize       parameter defining batch size for fetching role children for how many
   *                        roles in a go
   */
  public RoleChildrenFetcher(final RequestBuilder requestBuilder,
                             final RequestExecutor requestExecutor,
                             final int batchSize) {
    this.relatedItemsFetcher = RelatedItemsFetcher.<RoleRow, ChildRoleRow>builder()
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
        .entityTypeName("role children")
        .normalizeParentNames(true)
        .initializeEmptyLists(true)
        .build();
  }

  /**
   * Method to fetch and assign child roles for roles.
   *
   * @param roles List of roles whose child roles are to be fetched
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case api response is not ok/In case link header is malformed
   */
  public void populateRoleChildren(final List<RoleRow> roles)
      throws IOException, ServiceNowException {
    relatedItemsFetcher.populateRelatedItems(roles);
  }
}
