package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE;

import com.radiantlogic.enums.Group;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.GroupUserRequestBuilder;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
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
 * Handles group update requests by managing group-user relationships.
 *
 * <p>Currently supports only the GroupUsers attribute for adding and removing users from groups.
 * Other group attributes are not supported for modification.
 */
@Slf4j
public final class UpdateGroupRequestHandler implements UpdateHandler {

  /**
   * Manages group-user relationships.
   */
  private final GroupUserRelationshipManager groupUserRelationshipManager;

  /**
   * Constructs handler with group-user relationship manager.
   *
   * @param requestBuilder  Request builder for creating API requests
   * @param requestExecutor Executor for making API calls
   * @param batchSize       Batch size for fetching group users for how many groups in a go
   */
  public UpdateGroupRequestHandler(final RequestBuilder requestBuilder,
                                   final RequestExecutor requestExecutor,
                                   final int batchSize) {
    GroupUserRequestBuilder groupUserRequestBuilder =
        new GroupUserRequestBuilder(requestBuilder);
    this.groupUserRelationshipManager =
        new GroupUserRelationshipManager(groupUserRequestBuilder, requestExecutor, requestBuilder,
            batchSize);
  }

  /**
   * Checks if this handler supports the request.
   *
   * @param requestDetails details extracted from the modify request
   * @return {@code true} if the request is for a group entity
   */
  @Override
  public boolean supports(final RequestDetails requestDetails) {
    return requestDetails.isGroupRequest();
  }

  /**
   * Processes group update request by managing group-user relationships.
   *
   * @param groupName         name of the group to update
   * @param ldapModifyRequest LDAP modify request containing update details
   * @param schemaObject      schema definition for the group object
   * @throws ServiceNowException if attribute is invalid or not supported
   * @throws IOException         if a communication error occurs
   */
  @Override
  public void process(final String groupName,
                      final LdapModifyRequest ldapModifyRequest,
                      final SchemaObject schemaObject)
      throws ServiceNowException, IOException {
    processGroupUpdate(ldapModifyRequest, schemaObject, groupName);
  }

  /**
   * Processes group update by iterating through LDAP modifications.
   *
   * @param ldapModifyRequest Contains update details
   * @param schemaObject      SchemaObject
   * @param groupName         Group name
   * @throws ServiceNowException In case of invalid attribute or modification type
   * @throws IOException         if a communication error occurs
   */
  private void processGroupUpdate(final LdapModifyRequest ldapModifyRequest,
                                  final SchemaObject schemaObject,
                                  final String groupName)
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
      Group attributeReceivedForUpdate = Group.fromName(attributeName);
      processGroupUserRelationship(modificationType, attribute,
          attributeReceivedForUpdate, groupName);
    }
  }

  /**
   * Processes group-user relationship modifications by delegating to the relationship manager.
   *
   * @param modificationType           type of modification requested
   * @param attribute                  LDAP attribute being modified
   * @param attributeReceivedForUpdate resolved group attribute to be updated
   * @param groupName                  Group name
   * @throws ServiceNowException if attribute is not GroupUsers or modification type is invalid
   * @throws IOException         if a communication error occurs
   */
  private void processGroupUserRelationship(
      final Modification.ModificationType modificationType, final Attribute attribute,
      final Group attributeReceivedForUpdate, final String groupName)
      throws ServiceNowException, IOException {
    if (attributeReceivedForUpdate == Group.GROUP_USERS) {
      switch (modificationType) {
        case DELETE:
          groupUserRelationshipManager.processGroupUserDeletion(attribute, groupName);
          break;
        case ADD:
        case REPLACE:
          groupUserRelationshipManager.processGroupUserAddition(
              modificationType, attribute, groupName);
          break;
        default:
          throw new ServiceNowException(
              String.format("invalid modification type received: %s", modificationType.name()));
      }
    } else {
      throw new ServiceNowException(
          String.format("'%s' attribute modification not allowed for groups",
              attributeReceivedForUpdate.getAttributeName()));
    }
  }
}
