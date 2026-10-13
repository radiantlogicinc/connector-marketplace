package com.radiantlogic.get;

import com.google.gson.reflect.TypeToken;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.model.GroupRow;
import com.radiantlogic.model.GroupUserRow;
import com.radiantlogic.model.ServiceNowResponse;
import java.io.IOException;
import java.util.List;

/**
 * Fetches and assigns users for groups.
 */
public class GroupUsersFetcher {

  /**
   * Generic fetcher for group users.
   */
  private final RelatedItemsFetcher<GroupRow, GroupUserRow> relatedItemsFetcher;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  parameter to get request object for various objects & also credentials
   *                        checker
   * @param requestExecutor parameter to execute a request
   * @param batchSize       parameter defining batch size for fetching group users for how many
   *                        groups in a go
   */
  public GroupUsersFetcher(final RequestBuilder requestBuilder,
                           final RequestExecutor requestExecutor,
                           final int batchSize) {
    this.relatedItemsFetcher = RelatedItemsFetcher.<GroupRow, GroupUserRow>builder()
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
        .normalizeParentNames(true)
        .initializeEmptyLists(true)
        .build();
  }

  /**
   * Method to fetch and assign users for groups.
   *
   * @param groups List of groups whose users are to be fetched
   * @throws IOException         In case of connectivity errors
   * @throws ServiceNowException In case api response is not ok/In case link header is malformed
   */
  public void populateGroupUsers(final List<GroupRow> groups)
      throws IOException, ServiceNowException {
    relatedItemsFetcher.populateRelatedItems(groups);
  }
}
