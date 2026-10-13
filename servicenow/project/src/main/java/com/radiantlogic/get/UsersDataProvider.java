package com.radiantlogic.get;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import com.radiantlogic.utility.UserUtil;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Handler class responsible for fetching paginated user object data.
 */
@Slf4j
@Builder
public class UsersDataProvider implements SchemaObjectDataProvider {

  /**
   * Field used to get users data.
   */
  private final GetRequest getRequest;
  /**
   * Field used to convert response data.
   */
  private final SearchResponseConverter converter;
  /**
   * Field to store requested object's schema.
   */
  private final Schema schema;
  /**
   * Field to store requested object's details.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * Field to store user id in case of fetching a single record.
   */
  private final String uniqueKey;
  /**
   * Field to store url to get next set of records.
   */
  @Builder.Default
  private String nextLink = "";
  /**
   * Field to store whether more records exist.
   */
  @Builder.Default
  private boolean hasMoreRecordsFlag = true;
  /**
   * Field to store whether user roles should be fetched.
   */
  private final boolean isGetUserRoles;

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
    ServiceNowResponse<UserRow> serviceNowResponse =
        getRequest.getUsers(nextLink, uniqueKey, isGetUserRoles);
    this.nextLink = serviceNowResponse.getNextLink();
    this.hasMoreRecordsFlag = StringUtils.isNotBlank(nextLink);
    List<UserRow> users = serviceNowResponse.getResult();
    log.debug("users fetched: {}", users.size());
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    if (!users.isEmpty()) {
      pageEntries.addAll(
          converter.convert(UserUtil.extractUsersData(users, schema, targetSchemaObjects)));
    }
    return pageEntries;
  }
}
