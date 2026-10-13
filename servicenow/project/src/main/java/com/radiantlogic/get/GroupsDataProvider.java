package com.radiantlogic.get;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.GroupRow;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.utility.GroupUtil;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Handler class responsible for fetching paginated group object data.
 */
@Slf4j
@Builder
public class GroupsDataProvider implements SchemaObjectDataProvider {

  /**
   * Used to get groups data.
   */
  private final GetRequest getRequest;
  /**
   * Used to convert response data.
   */
  private final SearchResponseConverter converter;
  /**
   * Used to store requested object's schema.
   */
  private final Schema schema;
  /**
   * Used to store requested object's details.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * Used to store group name in case of fetching a single record.
   */
  private final String uniqueKey;
  /**
   * Used to store url to get next set of records.
   */
  @Builder.Default
  private String nextLink = "";
  /**
   * Used to store whether more records exist.
   */
  @Builder.Default
  private boolean hasMoreRecordsFlag = true;
  /**
   * Used to determine whether group users to be fetched.
   */
  private final boolean isGetGroupUsers;
  /**
   * Used to determine whether group roles to be fetched.
   */
  private final boolean isGetGroupRoles;

  /**
   * Returns url to get next set of paginated records.
   */
  @Override
  public String getNextLink() {
    return nextLink;
  }

  /**
   * Returns whether more records exist.
   */
  @Override
  public boolean hasMoreRecords() {
    return hasMoreRecordsFlag;
  }

  @Override
  public List<SearchResultEntry> getNextPage()
      throws IOException, ServiceNowException {
    ServiceNowResponse<GroupRow> serviceNowResponse =
        getRequest.getGroups(nextLink, uniqueKey, isGetGroupUsers, isGetGroupRoles);
    this.nextLink = serviceNowResponse.getNextLink();
    this.hasMoreRecordsFlag = StringUtils.isNotBlank(nextLink);
    List<GroupRow> groups = serviceNowResponse.getResult();
    log.debug("groups fetched: {}", groups.size());
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    if (!groups.isEmpty()) {
      pageEntries.addAll(
          converter.convert(GroupUtil.extractGroupsData(
              groups, schema, targetSchemaObjects)));
    }
    return pageEntries;
  }
}
