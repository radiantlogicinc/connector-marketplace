package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.GSON;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.UserRolesFetcher;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.model.MembershipCreationResponse;
import com.radiantlogic.model.MembershipQueryResponse;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Manages user-role relationships in ServiceNow. Handles adding and removing roles for users.
 *
 * <p><b>Design Rationale - Null Value Handling (ADD/REPLACE only):</b>
 * For ADD and REPLACE operations, this connector filters out null values from role assignments at
 * the connector level rather than relying on backend validation. This design decision was made
 * because:
 * <ul>
 *   <li>The backend does not properly handle null values in JSON arrays (e.g., ["admin", null])
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
 * <p><b>Documented Behavior for ADD/REPLACE:</b> If input is ["admin", null], only "admin" will
 * be linked. Pure-null arrays like [null] result in OPERATIONS_ERROR since no valid roles remain
 * after filtering.
 *
 * <p><b>DELETE Validation:</b> DELETE operations do NOT use the same normalization path as
 * ADD/REPLACE. The DELETE flow extract values directly without JSON-array parsing or null
 * filtering. Callers must ensure DELETE values are pre-normalized (no JSON-array strings, no
 * null/blank values). Invalid inputs (e.g., {@code ["[\"admin\"]"]}, {@code "null"}, or blank
 * strings) will be passed verbatim to the membership lookup API and result in "no membership
 * found" errors.
 */
@Slf4j
public final class UserRoleRelationshipManager {

  /**
   * Builds ServiceNow API requests.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Executes the constructed API requests.
   */
  private final RequestExecutor requestExecutor;

  /**
   * Fetches user roles.
   */
  private final UserRolesFetcher userRolesFetcher;

  /**
   * Constructor for UserRoleRelationshipManager.
   *
   * @param requestBuilder  Request builder for creating API requests
   * @param requestExecutor Executor for making API calls
   * @param batchSize       Batch size for fetching user roles for how many users in a go
   */
  public UserRoleRelationshipManager(final RequestBuilder requestBuilder,
                                     final RequestExecutor requestExecutor,
                                     final int batchSize) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
    this.userRolesFetcher = new UserRolesFetcher(requestBuilder, requestExecutor, batchSize);
  }

  /**
   * Processes user role modifications based on the modification type and attribute.
   *
   * <p><b>CRITICAL - Non-Atomic REPLACE Operation:</b>
   * REPLACE executes as: (1) delink all existing roles, (2) add each new role one-by-one. This is
   * NOT atomic and can leave the user in a degraded state:
   * <ul>
   *   <li>Step 1 (delink-all) is irreversible and always completes successfully</li>
   *   <li>Step 2 (add new roles) can fail partway through (e.g., network error, 500 response)</li>
   *   <li>If step 2 fails on the third role, the user permanently has only the first two new
   *       roles — fewer than either the original set or the intended replacement set</li>
   *   <li>The caller receives an error but cannot distinguish between "no roles added" vs
   *       "some roles added"</li>
   *   <li><b>Naive retry will fail</b> because the original roles are already gone</li>
   * </ul>
   *
   * <p><b>ADD Operation:</b>
   * ADD is also non-atomic but less dangerous because existing data is not destroyed first.
   * Partial failure leaves the user with original roles plus some (not all) new roles.
   *
   * <p><b>Recommendation:</b>
   * Avoid REPLACE for critical operations. Use explicit DELETE + ADD sequences with
   * application-level compensation logic if atomicity is required.
   *
   * <p><b>Input Normalization:</b>
   * Before processing, role names are filtered and normalized:
   * <ul>
   *   <li>Blank values (empty strings, whitespace-only) are filtered out</li>
   *   <li>Role names are converted to lowercase for case-insensitive matching</li>
   *   <li>Duplicates are removed (using Set)</li>
   * </ul>
   * If all values are blank after filtering, the operation fails with "no role added" error.
   *
   * @param modificationType Type of modification (ADD or REPLACE)
   * @param attribute        Attribute containing role values
   * @param userName         User name to whom roles will be assigned
   * @throws ServiceNowException if the API response is invalid/no role added
   * @throws IOException         if a communication error occurs
   */
  public void processUserRoleModification(final Modification.ModificationType modificationType,
                                          final Attribute attribute,
                                          final String userName)
      throws ServiceNowException, IOException {
    Set<String> rolesToProcess;
    if (modificationType == Modification.ModificationType.ADD) {
      rolesToProcess = Util.getValuesForModificationType(attribute);
    } else if (modificationType == Modification.ModificationType.REPLACE) {
      rolesToProcess = Util.getValuesForModificationType(attribute);
      delinkAllUserRoles(userName);
    } else {
      throw new ServiceNowException(
          String.format("invalid modification type received: %s", modificationType.name()));
    }
    int rolesAdded = 0;
    for (String roleName : rolesToProcess.stream()
        .filter(StringUtils::isNotBlank)
        .map(String::toLowerCase)
        .collect(Collectors.toSet())) {
      addRoleToUser(roleName, userName);
      log.info("linked role: {} with user:{}", roleName, userName);
      rolesAdded++;
    }
    if (rolesAdded == 0) {
      throw new ServiceNowException("no role added");
    }
    log.info("linked {} roles with user: {}", rolesAdded, userName);
  }

  /**
   * Processes deletion of all or multiple user roles.
   *
   * <p><b>Delete-All Operation:</b>
   * If attribute values are empty, all roles are deleted from the user.
   *
   * <p><b>Input Normalization:</b>
   * For specific role deletions, role names are filtered and normalized:
   * <ul>
   *   <li>Blank values (empty strings, whitespace-only) are filtered out</li>
   *   <li>Role names are converted to lowercase for case-insensitive matching</li>
   *   <li>Duplicates are removed (using Set)</li>
   * </ul>
   * If all values are blank after filtering, no deletions are performed and the operation
   * succeeds silently.
   *
   * @param attribute Attribute containing role values to delete
   * @param userName  User name from whom roles will be removed
   * @throws ServiceNowException if the API response is invalid or membership not found
   * @throws IOException         if a communication error occurs
   */
  public void processUserRoleDeletion(final Attribute attribute, final String userName)
      throws ServiceNowException, IOException {
    if (attribute.getValues().isEmpty()) {
      delinkAllUserRoles(userName);
    } else {
      for (String roleName : attribute.getValues().stream()
          .filter(StringUtils::isNotBlank)
          .map(String::toLowerCase)
          .collect(Collectors.toSet())) {
        delinkRoleFromUser(userName, roleName);
        log.info("delinked role: {} from user: {}", roleName, userName);
      }
    }
  }

  /**
   * Deletes all roles for a given user by first fetching all user roles and then removing each role
   * membership.
   *
   * @param userName User name from whom all roles will be removed
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if no roles found or membership deletion fails
   */
  private void delinkAllUserRoles(final String userName) throws IOException, ServiceNowException {
    UserRow userRow = new UserRow();
    userRow.setUsername(userName);
    List<UserRow> users = Collections.singletonList(userRow);
    userRolesFetcher.populateUserRoles(users);
    List<String> userRoles = userRow.getUserRoles();
    for (String roleName : userRoles) {
      delinkRoleFromUser(userName, roleName);
      log.info("delinked role: {} from user: {}", roleName, userName);
    }
    log.info("Successfully delinked {} roles for user: {}", userRoles.size(), userName);
  }

  /**
   * Removes a specific role from a user by deleting the user-role membership.
   *
   * @param userName User name from whom the role will be removed
   * @param roleName Role name to be removed
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if membership not found or deletion fails
   */
  private void delinkRoleFromUser(final String userName, final String roleName)
      throws IOException, ServiceNowException {
    String membershipId = getUserRoleMembershipId(userName, roleName);
    if (StringUtils.isNotBlank(membershipId)) {
      deleteMembership(membershipId);
      return;
    }
    throw new ServiceNowException(
        String.format("no membership found for the user: %s and the role: %s", userName,
            roleName));
  }

  /**
   * Gets the membership sys_id for a user-role relationship.
   *
   * @param userName User name
   * @param roleName Role name
   * @return Membership sys_id or null if not found
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if the API response is invalid
   */
  private String getUserRoleMembershipId(final String userName, final String roleName)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getUserRoleMembershipRequest(userName, roleName));
    int responseCode = responseData.getResponseCode();
    log.debug("get user role membership api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.OK.asInt()) {
      throw new ServiceNowException(
          String.format("get user role membership api response message is: %s", apiResponse));
    }
    MembershipQueryResponse response =
        GSON.fromJson(apiResponse, MembershipQueryResponse.class);
    if (response == null || response.getResult() == null || response.getResult().isEmpty()) {
      log.warn("No membership found for user '{}' and role '{}'", userName, roleName);
      return null;
    }
    String membershipId = response.getResult().get(0).getSysId();
    log.debug("Found membership ID: {}", membershipId);
    return membershipId;
  }

  /**
   * Deletes a user-role membership by its sys_id.
   *
   * @param membershipId System ID of the membership to delete
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if the API response is not 204 No Content
   */
  private void deleteMembership(final String membershipId)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getDeleteUserRoleMembershipRequest(membershipId));
    int responseCode = responseData.getResponseCode();
    log.debug("delete membership api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());

    // even with a valid membership id, may be forbidden if inherited role, say if inherited roles
    // come with admin role, then upon deleting that inherited role will give 403 acl not allowed
    if (responseCode != HttpStatus.NO_CONTENT.asInt()) {
      String apiResponse = responseData.getApiResponse();
      throw new ServiceNowException(
          String.format("delete membership api failed with response: %s", apiResponse));
    }
    log.info("Successfully deleted membership with ID: {}", membershipId);
  }

  /**
   * Adds a role to a user by invoking the ServiceNow API.
   *
   * @param roleName Role name to be assigned to the user
   * @param userName User name to whom the role will be assigned
   * @throws IOException         if a communication error occurs while calling the API
   * @throws ServiceNowException if the API response is not 201 or sys_id is blank
   */
  private void addRoleToUser(final String roleName, final String userName)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getAddRoleToUserRequest(roleName, userName));
    int responseCode = responseData.getResponseCode();
    log.debug("add role to user api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.CREATED.asInt()) {
      throw new ServiceNowException(
          String.format("add role to user api response message is: %s", apiResponse));
    }
    MembershipCreationResponse response =
        GSON.fromJson(apiResponse, MembershipCreationResponse.class);
    if (response == null || response.getResult() == null
        || StringUtils.isBlank(response.getResult().getSysId())) {
      throw new ServiceNowException(
          String.format(
              "add role to user api response does not contain valid sys_id: %s/ role might be "
                  + "already linked",
              apiResponse));
    }
    log.info("Successfully added role '{}' to user '{}' with sys_id: {}",
        roleName, userName, response.getResult().getSysId());
  }
}
