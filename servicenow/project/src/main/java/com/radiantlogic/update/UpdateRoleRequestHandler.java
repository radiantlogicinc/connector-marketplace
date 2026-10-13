package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE;

import com.radiantlogic.enums.Role;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.http.RoleChildRequestBuilder;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.interfaces.UpdateHandler;
import com.radiantlogic.model.RequestDetails;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Handles role update requests by managing role-child role relationships.
 *
 * <p>Currently supports only the ChildRoles attribute for adding and removing child roles from
 * parent roles. Other role attributes are not supported for modification.
 */
@Slf4j
public final class UpdateRoleRequestHandler implements UpdateHandler {

  /**
   * Manages role-child role relationships.
   */
  private final RoleChildRelationshipManager roleChildRelationshipManager;

  /**
   * Constructs handler with role-child relationship manager.
   *
   * @param requestBuilder  Request builder for creating API requests
   * @param requestExecutor Executor for making API calls
   * @param batchSize       Batch size for fetching role children for how many roles in a go
   */
  public UpdateRoleRequestHandler(final RequestBuilder requestBuilder,
                                  final RequestExecutor requestExecutor,
                                  final int batchSize) {
    RoleChildRequestBuilder roleChildRequestBuilder =
        new RoleChildRequestBuilder(requestBuilder);
    this.roleChildRelationshipManager =
        new RoleChildRelationshipManager(roleChildRequestBuilder, requestExecutor, requestBuilder,
            batchSize);
  }

  /**
   * Checks if this handler supports the request.
   *
   * @param requestDetails details extracted from the modify request
   * @return {@code true} if the request is for a role entity
   */
  @Override
  public boolean supports(final RequestDetails requestDetails) {
    return requestDetails.isRoleRequest();
  }

  /**
   * Processes role update request by managing role-child role relationships.
   *
   * @param roleName          name of the role to update
   * @param ldapModifyRequest LDAP modify request containing update details
   * @param schemaObject      schema definition for the role object
   * @throws ServiceNowException if attribute is invalid or not supported
   * @throws IOException         if a communication error occurs
   */
  @Override
  public void process(final String roleName,
                      final LdapModifyRequest ldapModifyRequest,
                      final SchemaObject schemaObject)
      throws ServiceNowException, IOException {
    processRoleUpdate(ldapModifyRequest, schemaObject, roleName);
  }

  /**
   * Processes role update by iterating through LDAP modifications.
   *
   * @param ldapModifyRequest Contains update details
   * @param schemaObject      SchemaObject
   * @param roleName          Role name
   * @throws ServiceNowException In case of invalid attribute or modification type
   * @throws IOException         if a communication error occurs
   */
  private void processRoleUpdate(final LdapModifyRequest ldapModifyRequest,
                                 final SchemaObject schemaObject,
                                 final String roleName)
      throws ServiceNowException, IOException {
    for (Modification modification : ldapModifyRequest.getModifications()) {
      Attribute attribute = modification.getAttribute();
      Modification.ModificationType modificationType = modification.getModificationType();
      log.debug(ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE, attribute.getName(),
          attribute.getValues(), modificationType.name());
      String attributeName = Util.getAttributeName(schemaObject, attribute.getName());
      if (StringUtils.isBlank(attributeName)) {
        throw new ServiceNowException(
            String.format("invalid attribute received for update: %s", attribute.getName()));
      }
      Role attributeReceivedForUpdate = Role.fromName(attributeName);
      processRoleChildRelationship(modificationType, attribute,
          attributeReceivedForUpdate, roleName);
    }
  }

  /**
   * Processes role-child role relationship modifications by delegating to the relationship
   * manager.
   *
   * @param modificationType           type of modification requested
   * @param attribute                  LDAP attribute being modified
   * @param attributeReceivedForUpdate resolved role attribute to be updated
   * @param roleName                   Role name
   * @throws ServiceNowException if attribute is not ChildRoles or modification type is invalid
   * @throws IOException         if a communication error occurs
   */
  private void processRoleChildRelationship(
      final Modification.ModificationType modificationType, final Attribute attribute,
      final Role attributeReceivedForUpdate, final String roleName)
      throws ServiceNowException, IOException {
    if (attributeReceivedForUpdate == Role.CHILD_ROLES) {
      switch (modificationType) {
        case DELETE:
          roleChildRelationshipManager.processRoleChildDeletion(attribute, roleName);
          break;
        case ADD:
        case REPLACE:
          roleChildRelationshipManager.processRoleChildAddition(
              modificationType, attribute, roleName);
          break;
        default:
          throw new ServiceNowException(
              String.format("invalid modification type received: %s", modificationType.name()));
      }
    } else {
      throw new ServiceNowException(
          String.format("'%s' attribute modification not allowed for roles",
              attributeReceivedForUpdate.getAttributeName()));
    }
  }
}
