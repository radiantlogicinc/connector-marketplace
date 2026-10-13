package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.GSON;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.RoleChildrenFetcher;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.http.RoleChildRequestBuilder;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.model.MembershipCreationResponse;
import com.radiantlogic.model.MembershipQueryResponse;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.RoleRow;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Manages role-child relationships in ServiceNow. Handles adding and removing child roles for
 * roles.
 *
 * <p><b>Design Rationale - Null Value Handling (ADD/REPLACE only):</b>
 * For ADD and REPLACE operations, this connector filters out null values from child role
 * assignments at the connector level rather than relying on backend validation. This design
 * decision was made because:
 * <ul>
 *   <li>The backend does not properly handle null values in JSON arrays (e.g., ["childrole1",
 *   null])
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
 * <p><b>Documented Behavior for ADD/REPLACE:</b> If input is ["childrole1", null], only
 * "childrole1" will be linked. Pure-null arrays like [null] result in OPERATIONS_ERROR since no
 * valid child roles remain after filtering.
 *
 * <p><b>DELETE Validation:</b> DELETE operations do NOT use the same normalization path as
 * ADD/REPLACE. The DELETE flow extract values directly without
 * JSON-array parsing or null filtering. Callers must ensure DELETE values are pre-normalized
 * (no JSON-array strings, no null/blank values). Invalid inputs (e.g., {@code ["[\"childrole1
 * \"]"]}, {@code "null"}, or blank strings) will be passed verbatim to the membership lookup API
 * and result in "no membership found" errors.
 */
@Slf4j
public final class RoleChildRelationshipManager {

  /**
   * Builds ServiceNow API requests for role-child role operations.
   */
  private final RoleChildRequestBuilder roleChildRequestBuilder;

  /**
   * Executes the constructed API requests.
   */
  private final RequestExecutor requestExecutor;

  /**
   * Fetches role children.
   */
  private final RoleChildrenFetcher roleChildrenFetcher;

  /**
   * Constructor for RoleChildRelationshipManager.
   *
   * @param roleChildRequestBuilder Request builder for creating API requests
   * @param requestExecutor         Executor for making API calls
   * @param requestBuilder          Request builder for fetching role children
   * @param batchSize               Batch size for fetching role children for how many roles in a
   *                                go
   */
  public RoleChildRelationshipManager(final RoleChildRequestBuilder roleChildRequestBuilder,
                                      final RequestExecutor requestExecutor,
                                      final RequestBuilder requestBuilder,
                                      final int batchSize) {
    this.roleChildRequestBuilder = roleChildRequestBuilder;
    this.requestExecutor = requestExecutor;
    this.roleChildrenFetcher = new RoleChildrenFetcher(requestBuilder, requestExecutor,
        batchSize);
  }

  /**
   * Adds child roles to a parent role based on modification type.
   *
   * <p><b>CRITICAL - Non-Atomic REPLACE Operation:</b>
   * REPLACE executes as: (1) delink all existing child roles, (2) add each new child role
   * one-by-one. This is NOT atomic and can leave the parent role in a degraded state:
   * <ul>
   *   <li>Step 1 (delink-all) is irreversible and always completes successfully</li>
   *   <li>Step 2 (add new child roles) can fail partway through (e.g., network error, 500
   *       response)</li>
   *   <li>If step 2 fails on the third child role, the parent role permanently has only the first
   *       two new child roles — fewer than either the original set or the intended replacement
   *       set</li>
   *   <li>The caller receives an error but cannot distinguish between "no child roles added" vs
   *       "some child roles added"</li>
   *   <li><b>Naive retry will fail</b> because the original child roles are already gone</li>
   * </ul>
   *
   * <p><b>ADD Operation:</b>
   * ADD is also non-atomic but less dangerous because existing data is not destroyed first.
   * Partial failure leaves the parent role with original child roles plus some (not all) new
   * child roles.
   *
   * <p><b>Recommendation:</b>
   * Avoid REPLACE for critical operations. Use explicit DELETE + ADD sequences with
   * application-level compensation logic if atomicity is required.
   *
   * <p><b>Input Normalization:</b>
   * Before processing, child role names are filtered and normalized:
   * <ul>
   *   <li>Blank values (empty strings, whitespace-only) are filtered out</li>
   *   <li>Role names are converted to lowercase for case-insensitive matching</li>
   *   <li>Duplicates are removed (using Set)</li>
   * </ul>
   * If all values are blank after filtering, the operation fails with "no child role added" error.
   *
   * @param modificationType Type of modification (ADD or REPLACE)
   * @param attribute        Attribute containing child role values
   * @param parentRoleName   Parent role name to which child roles will be added
   * @throws ServiceNowException if the API response is invalid/no child role added
   * @throws IOException         if a communication error occurs
   */
  public void processRoleChildAddition(final Modification.ModificationType modificationType,
                                       final Attribute attribute,
                                       final String parentRoleName)
      throws ServiceNowException, IOException {
    Set<String> childRolesToProcess;
    if (modificationType == Modification.ModificationType.ADD) {
      childRolesToProcess = Util.getValuesForModificationType(attribute);
    } else if (modificationType == Modification.ModificationType.REPLACE) {
      childRolesToProcess = Util.getValuesForModificationType(attribute);
      delinkAllChildRolesFromRole(parentRoleName);
    } else {
      throw new ServiceNowException(
          String.format("invalid modification type received: %s", modificationType.name()));
    }
    int childRolesAdded = 0;
    for (String childRoleName : childRolesToProcess.stream()
        .filter(StringUtils::isNotBlank)
        .map(String::toLowerCase)
        .collect(Collectors.toSet())) {
      addChildRole(parentRoleName, childRoleName);
      log.info("linked child role: {} with role:{}", childRoleName, parentRoleName);
      childRolesAdded++;
    }
    if (childRolesAdded == 0) {
      throw new ServiceNowException("no child role added");
    }
    log.info("linked {} child roles with parent role '{}'", childRolesAdded, parentRoleName);
  }

  /**
   * Processes deletion of all or multiple role's child roles.
   *
   * <p><b>Delete-All Operation:</b>
   * If attribute values are empty, all child roles are deleted from the parent role.
   *
   * <p><b>Input Normalization:</b>
   * For specific child role deletions, role names are filtered and normalized:
   * <ul>
   *   <li>Blank values (empty strings, whitespace-only) are filtered out</li>
   *   <li>Role names are converted to lowercase for case-insensitive matching</li>
   *   <li>Duplicates are removed (using Set)</li>
   * </ul>
   * If all values are blank after filtering, no deletions are performed and the operation
   * succeeds silently.
   *
   * @param attribute      Attribute containing child role values to delete
   * @param parentRoleName Parent role name from which child roles will be removed
   * @throws ServiceNowException if membership not found or API call fails
   * @throws IOException         if a communication error occurs
   */
  public void processRoleChildDeletion(final Attribute attribute, final String parentRoleName)
      throws ServiceNowException, IOException {
    if (attribute.getValues().isEmpty()) {
      delinkAllChildRolesFromRole(parentRoleName);
    } else {
      for (String roleName : attribute.getValues().stream()
          .filter(StringUtils::isNotBlank)
          .map(String::toLowerCase)
          .collect(Collectors.toSet())) {
        delinkChildRoleFromRole(parentRoleName, roleName);
        log.info("delinked child role: {} from role: {}", roleName, parentRoleName);
      }
    }
  }

  /**
   * Removes all child roles from a parent role.
   *
   * @param parentRoleName Parent role name from which all child roles will be removed
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if no child roles found or API call fails
   */
  private void delinkAllChildRolesFromRole(final String parentRoleName)
      throws IOException, ServiceNowException {
    RoleRow roleRow = new RoleRow();
    roleRow.setName(parentRoleName);
    List<RoleRow> roles = Collections.singletonList(roleRow);
    roleChildrenFetcher.populateRoleChildren(roles);
    List<String> childRoles = roleRow.getChildRoles();
    for (String childRoleName : childRoles) {
      delinkChildRoleFromRole(parentRoleName, childRoleName);
      log.info("delinked child role: {} from role: {}", childRoleName, parentRoleName);
    }
    log.info("Successfully delinked {} child roles from parent role: {}", childRoles.size(),
        parentRoleName);
  }

  /**
   * Removes a specific child role from a parent role.
   *
   * @param parentRoleName Parent role name from which child role will be removed
   * @param childRoleName  Child role name to remove
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if membership not found or API call fails
   */
  private void delinkChildRoleFromRole(final String parentRoleName, final String childRoleName)
      throws IOException, ServiceNowException {
    String membershipId = getRoleChildMembershipId(parentRoleName, childRoleName);
    if (StringUtils.isNotBlank(membershipId)) {
      deleteMembership(membershipId);
      return;
    }
    throw new ServiceNowException(
        String.format("no membership found for the parent role: %s and the child role: %s",
            parentRoleName, childRoleName));
  }

  /**
   * Retrieves membership sys_id for a role-child role relationship.
   *
   * @param parentRoleName Parent role name
   * @param childRoleName  Child role name
   * @return Membership sys_id or null if not found
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if API call fails
   */
  private String getRoleChildMembershipId(final String parentRoleName, final String childRoleName)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(
            roleChildRequestBuilder.getRoleChildMembershipRequest(parentRoleName, childRoleName));
    int responseCode = responseData.getResponseCode();
    log.debug("get role child membership api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.OK.asInt()) {
      throw new ServiceNowException(
          String.format("get role child membership api response message is: %s", apiResponse));
    }
    MembershipQueryResponse response =
        GSON.fromJson(apiResponse, MembershipQueryResponse.class);
    if (response == null || response.getResult() == null || response.getResult().isEmpty()) {
      log.warn("No membership found for parent role '{}' and child role '{}'",
          parentRoleName, childRoleName);
      return null;
    }
    String membershipId = response.getResult().get(0).getSysId();
    log.debug("Found membership ID: {}", membershipId);
    return membershipId;
  }

  /**
   * Deletes a role-child role membership.
   *
   * @param membershipId System ID of the membership to delete
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if API call fails
   */
  private void deleteMembership(final String membershipId)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(
            roleChildRequestBuilder.getDeleteRoleChildMembershipRequest(membershipId));
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
   * Adds a child role to a parent role.
   *
   * @param parentRoleName Parent role name
   * @param childRoleName  Child role name to add
   * @throws IOException         if a communication error occurs
   * @throws ServiceNowException if API call fails or returns invalid response
   */
  private void addChildRole(final String parentRoleName, final String childRoleName)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(
            roleChildRequestBuilder.getAddChildRoleRequest(parentRoleName, childRoleName));
    int responseCode = responseData.getResponseCode();
    log.debug("add child role api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.CREATED.asInt()) {
      throw new ServiceNowException(
          String.format("add child role api response message is: %s", apiResponse));
    }
    MembershipCreationResponse response =
        GSON.fromJson(apiResponse, MembershipCreationResponse.class);
    if (response == null || response.getResult() == null
        || StringUtils.isBlank(response.getResult().getSysId())) {
      throw new ServiceNowException(
          String.format(
              "add child role api response does not contain valid sys_id: %s/ membership might "
                  + "be already linked",
              apiResponse));
    }
    log.info("Successfully added child role '{}' to parent role '{}' with sys_id: {}",
        childRoleName, parentRoleName, response.getResult().getSysId());
  }
}
