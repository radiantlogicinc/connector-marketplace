package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.GSON;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GroupUsersFetcher;
import com.radiantlogic.http.GroupUserRequestBuilder;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.model.GroupRow;
import com.radiantlogic.model.MembershipCreationResponse;
import com.radiantlogic.model.MembershipQueryResponse;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Manages group-user relationships in ServiceNow. Handles adding and removing users for groups.
 *
 * <p><b>Design Rationale - Null Value Handling (ADD/REPLACE only):</b>
 * For ADD and REPLACE operations, this connector filters out null values from user assignments at
 * the connector level rather than relying on backend validation. This design decision was made
 * because:
 * <ul>
 *   <li>The backend does not properly handle null values in JSON arrays (e.g., ["user1", null])
 *   </li>
 *   <li>Filtering at the connector level provides consistent, predictable behavior</li>
 *   <li>It prevents invalid API calls that would fail on the backend</li>
 * </ul>
 *
 * <p><b>Enforcement Locations:</b>
 * <ul>
 *   <li>{@link Util#getValuesForModificationType} - filters nulls
 *       via stream operation for ADD & REPLACE modifications</li>
 * </ul>
 *
 * <p><b>Documented Behavior for ADD/REPLACE:</b> If input is ["user1", null], only "user1" will
 * be linked. Pure-null arrays like [null] result in OPERATIONS_ERROR since no valid users remain
 * after filtering.
 *
 * <p><b>DELETE Validation:</b> DELETE operations do NOT use the same normalization path as
 * ADD/REPLACE. The DELETE flow extract values directly without JSON-array parsing or null
 * filtering. Callers must ensure DELETE values are pre-normalized (no JSON-array strings, no
 * null/blank values). Invalid inputs (e.g., {@code ["[\"user1\"]"]}, {@code "null"}, or blank
 * strings) will be passed verbatim to the membership lookup API and result in "no membership
 * found" errors.
 */
@Slf4j
public final class GroupUserRelationshipManager {

  /**
   * Builds ServiceNow API requests for group-user operations.
   */
  private final GroupUserRequestBuilder groupUserRequestBuilder;

  /**
   * Executes the constructed API requests.
   */
  private final RequestExecutor requestExecutor;

  /**
   * Fetches group users.
   */
  private final GroupUsersFetcher groupUsersFetcher;

  /**
   * Constructor for GroupUserRelationshipManager.
   *
   * @param groupUserRequestBuilder Request builder for creating API requests
   * @param requestExecutor         Executor for making API calls
   * @param requestBuilder          Request builder for fetching group users
   * @param batchSize               Batch size for fetching group users for how many groups in a go
   */
  public GroupUserRelationshipManager(final GroupUserRequestBuilder groupUserRequestBuilder,
                                      final RequestExecutor requestExecutor,
                                      final RequestBuilder requestBuilder,
                                      final int batchSize) {
    this.groupUserRequestBuilder = groupUserRequestBuilder;
    this.requestExecutor = requestExecutor;
    this.groupUsersFetcher = new GroupUsersFetcher(requestBuilder, requestExecutor, batchSize);
  }

  /**
   * Adds users to a group based on modification type.
   *
   * <p><b>CRITICAL - Non-Atomic REPLACE Operation:</b>
   * REPLACE executes as: (1) delink all existing users, (2) add each new user one-by-one. This is
   * NOT atomic and can leave the group in a degraded state:
   * <ul>
   *   <li>Step 1 (delink-all) is irreversible and always completes successfully</li>
   *   <li>Step 2 (add new users) can fail partway through (e.g., network error, 500 response)</li>
   *   <li>If step 2 fails on the third user, the group permanently has only the first two new
   *       users — fewer than either the original set or the intended replacement set</li>
   *   <li>The caller receives an error but cannot distinguish between "no users added" vs
   *       "some users added"</li>
   *   <li><b>Naive retry will fail</b> because the original users are already gone</li>
   * </ul>
   *
   * <p><b>ADD Operation:</b>
   * ADD is also non-atomic but less dangerous because existing data is not destroyed first.
   * Partial failure leaves the group with original users plus some (not all) new users.
   *
   * <p><b>Recommendation:</b>
   * Avoid REPLACE for critical operations. Use explicit DELETE + ADD sequences with
   * application-level compensation logic if atomicity is required.
   *
   * <p><b>Input Normalization:</b>
   * Before processing, user names are filtered and normalized:
   * <ul>
   *   <li>Blank values (empty strings, whitespace-only) are filtered out</li>
   *   <li>User names are converted to lowercase for case-insensitive matching</li>
   *   <li>Duplicates are removed (using Set)</li>
   * </ul>
   * If all values are blank after filtering, the operation fails with "no user added" error.
   *
   * @param modificationType Type of modification (ADD or REPLACE)
   * @param attribute        Attribute containing user values
   * @param groupName        Group name to which users will be added
   * @throws ServiceNowException if the API response is invalid/no user added
   * @throws IOException         if a communication error occurs
   */
  public void processGroupUserAddition(final Modification.ModificationType modificationType,
                                       final Attribute attribute,
                                       final String groupName)
      throws ServiceNowException, IOException {
    Set<String> usersToProcess;
    if (modificationType == Modification.ModificationType.ADD) {
      usersToProcess = Util.getValuesForModificationType(attribute);
    } else if (modificationType == Modification.ModificationType.REPLACE) {
      usersToProcess = Util.getValuesForModificationType(attribute);
      delinkAllUsersFromGroup(groupName);
    } else {
      throw new ServiceNowException(
          String.format("invalid modification type received: %s", modificationType.name()));
    }
    int usersAdded = 0;
    for (String userName : usersToProcess.stream()
        .filter(StringUtils::isNotBlank)
        .map(String::toLowerCase)
        .collect(Collectors.toSet())) {
      addUserToGroup(userName, groupName);
      log.info("linked user: {} with group:{}", userName, groupName);
      usersAdded++;
    }
    if (usersAdded == 0) {
      throw new ServiceNowException("no user added");
    }
    log.info("added {} users with group '{}'", usersAdded, groupName);
  }

  /**
   * Processes deletion of all or multiple group users.
   *
   * <p><b>Delete-All Operation:</b>
   * If attribute values are empty, all users are deleted from the group.
   *
   * <p><b>Input Normalization:</b>
   * For specific user deletions, user names are filtered and normalized:
   * <ul>
   *   <li>Blank values (empty strings, whitespace-only) are filtered out</li>
   *   <li>User names are converted to lowercase for case-insensitive matching</li>
   *   <li>Duplicates are removed (using Set)</li>
   * </ul>
   * If all values are blank after filtering, no deletions are performed and the operation
   * succeeds silently.
   *
   * @param attribute Attribute containing user values to delete
   * @param groupName Group name from which users will be removed
   * @throws ServiceNowException if membership not found or API call fails
   * @throws IOException         if a communication error occurs
   */
  public void processGroupUserDeletion(final Attribute attribute, final String groupName)
      throws ServiceNowException, IOException {
    if (attribute.getValues().isEmpty()) {
      delinkAllUsersFromGroup(groupName);
    } else {
      for (String userName : attribute.getValues().stream()
          .filter(StringUtils::isNotBlank)
          .map(String::toLowerCase)
          .collect(Collectors.toSet())) {
        delinkUserFromGroup(userName, groupName);
        log.info("delinked user: {} from group: {}", userName, groupName);
      }
    }
  }

  /**
   * Removes all users from a group.
   *
   * @param groupName Group name from which all users will be removed
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if no users found or API call fails
   */
  private void delinkAllUsersFromGroup(final String groupName)
      throws IOException, ServiceNowException {
    GroupRow groupRow = new GroupRow();
    groupRow.setName(groupName);
    List<GroupRow> groups = Collections.singletonList(groupRow);
    groupUsersFetcher.populateGroupUsers(groups);
    List<String> groupUsers = groupRow.getGroupUsers();
    for (String userName : groupUsers) {
      delinkUserFromGroup(userName, groupName);
      log.info("delinked user: {} from group: {}", userName, groupName);
    }
    log.info("Successfully delinked {} users from group: {}", groupUsers.size(), groupName);
  }

  /**
   * Removes a specific user from a group.
   *
   * @param userName  User name to remove
   * @param groupName Group name from which user will be removed
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if membership not found or API call fails
   */
  private void delinkUserFromGroup(final String userName, final String groupName)
      throws IOException, ServiceNowException {
    String membershipId = getUserGroupMembershipId(userName, groupName);
    if (StringUtils.isNotBlank(membershipId)) {
      deleteMembership(membershipId);
      return;
    }
    throw new ServiceNowException(
        String.format("no membership found for the user: %s and the group: %s", userName,
            groupName));
  }

  /**
   * Retrieves membership sys_id for a user-group relationship.
   *
   * @param userName  User name
   * @param groupName Group name
   * @return Membership sys_id or null if not found
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if API call fails
   */
  private String getUserGroupMembershipId(final String userName, final String groupName)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(
            groupUserRequestBuilder.getUserGroupMembershipRequest(userName, groupName));
    int responseCode = responseData.getResponseCode();
    log.debug("get user group membership api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.OK.asInt()) {
      throw new ServiceNowException(
          String.format("get user group membership api response message is: %s", apiResponse));
    }
    MembershipQueryResponse response =
        GSON.fromJson(apiResponse, MembershipQueryResponse.class);
    if (response == null || response.getResult() == null || response.getResult().isEmpty()) {
      log.warn("No membership found for user '{}' and group '{}'", userName, groupName);
      return null;
    }
    String membershipId = response.getResult().get(0).getSysId();
    log.debug("Found membership ID: {}", membershipId);
    return membershipId;
  }

  /**
   * Deletes a user-group membership.
   *
   * @param membershipId System ID of the membership to delete
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if API call fails
   */
  private void deleteMembership(final String membershipId)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(
            groupUserRequestBuilder.getDeleteUserGroupMembershipRequest(membershipId));
    int responseCode = responseData.getResponseCode();
    log.debug("delete membership api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());

    if (responseCode != HttpStatus.NO_CONTENT.asInt()) {
      String apiResponse = responseData.getApiResponse();
      throw new ServiceNowException(
          String.format("delete membership api failed with response: %s", apiResponse));
    }
    log.info("Successfully deleted membership with ID: {}", membershipId);
  }

  /**
   * Adds a user to a group.
   *
   * @param userName  User name to add
   * @param groupName Group name
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if API call fails or returns invalid response
   */
  private void addUserToGroup(final String userName, final String groupName)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(
            groupUserRequestBuilder.getAddUserToGroupRequest(groupName, userName));
    int responseCode = responseData.getResponseCode();
    log.debug("add user to group api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.CREATED.asInt()) {
      throw new ServiceNowException(
          String.format("add user to group api response message is: %s", apiResponse));
    }
    MembershipCreationResponse response =
        GSON.fromJson(apiResponse, MembershipCreationResponse.class);
    if (response == null || response.getResult() == null
        || StringUtils.isBlank(response.getResult().getSysId())) {
      throw new ServiceNowException(
          String.format(
              "add user to group api response does not contain valid sys_id: %s/ membership might "
                  + "be already linked",
              apiResponse));
    }
    log.info("Successfully added user '{}' to group '{}' with sys_id: {}",
        userName, groupName, response.getResult().getSysId());
  }
}
