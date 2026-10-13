package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE;
import static com.radiantlogic.utility.Constants.GSON;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.enums.User;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.interfaces.UpdateHandler;
import com.radiantlogic.model.RequestDetails;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link UpdateHandler} implementation responsible for processing update requests for user
 * objects.
 *
 * <p>This handler prepares the user update payload from the incoming LDAP modify
 * request and invokes the ServiceNow API to perform the update operation.
 */
@Slf4j
public final class UpdateUserRequestHandler implements UpdateHandler {

  /**
   * Builds ServiceNow API requests for user operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Executes the constructed API requests.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Field to get data for various objects.
   */
  private final GetRequest getRequest;
  /**
   * Manages user-role relationships.
   */
  private final UserRoleRelationshipManager userRoleRelationshipManager;

  /**
   * Constructor for UpdateUserRequestHandler.
   *
   * @param requestBuilder  Request builder for creating API requests
   * @param requestExecutor Executor for making API calls
   * @param getRequest      Field to get data for various objects
   * @param batchSize       Batch size for fetching user roles for how many users in a go
   */
  public UpdateUserRequestHandler(final RequestBuilder requestBuilder,
                                  final RequestExecutor requestExecutor,
                                  final GetRequest getRequest,
                                  final int batchSize) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
    this.getRequest = getRequest;
    this.userRoleRelationshipManager =
        new UserRoleRelationshipManager(requestBuilder, requestExecutor, batchSize);
  }

  /**
   * Determines whether this handler supports the provided request.
   *
   * @param requestDetails details extracted from the modify request
   * @return {@code true} if the request corresponds to a user update
   */
  @Override
  public boolean supports(final RequestDetails requestDetails) {
    return requestDetails.isUserRequest();
  }

  /**
   * Processes the user update request by preparing the user payload and invoking the update
   * operation.
   *
   * @param username          username of the user to update
   * @param ldapModifyRequest LDAP modify request containing update details
   * @param schemaObject      schema definition for the user object
   * @throws ServiceNowException In case of invalid attribute or modification type, multivalued
   *                             attribute, attributes which cannot be set/updated/kept
   *                             blank/deleted/invalid identity type value/invalid boolean string
   *                             value/if the attribute does not support direct setting/api response
   *                             not ok/api response contains no user entries/invalid
   *                             username/username attribute received for update
   * @throws IOException         if a communication error occurs while invoking the API/no role
   *                             added
   */
  @Override
  public void process(final String username,
                      final LdapModifyRequest ldapModifyRequest,
                      final SchemaObject schemaObject)
      throws ServiceNowException, IOException {
    UserRow userRow = prepareUserObjectForUpdate(ldapModifyRequest, schemaObject, username);
    if (!"{}".equals(GSON.toJson(userRow))) {
      ServiceNowResponse<UserRow> usersResponse = getRequest.getUsers(null, username, false);
      List<UserRow> users = usersResponse.getResult();
      if (users.isEmpty()) {
        throw new ServiceNowException(String.format("no user found with username: %s", username));
      }
      String sysId = users.get(0).getSysId();
      updateUser(userRow, sysId);
    }
  }

  /**
   * Creates user object for update user.
   *
   * @param ldapModifyRequest Contains update details
   * @param schemaObject      SchemaObject
   * @return UserRow Contains values to be updated
   * @throws ServiceNowException In case of invalid attribute or modification type, multivalued
   *                             attribute, attributes which cannot be set/updated/kept
   *                             blank/deleted/invalid identity type value/invalid boolean string
   *                             value/if the attribute does not support direct setting/no role
   *                             added/username attribute received for update
   */
  @NonNull
  private UserRow prepareUserObjectForUpdate(final LdapModifyRequest ldapModifyRequest,
                                             final SchemaObject schemaObject, final String userName)
      throws ServiceNowException, IOException {
    UserRow userRow = new UserRow();
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
      User attributeReceivedForUpdate = User.fromName(attributeName);
      prepareUserObjectForModification(modificationType, attribute,
          attributeReceivedForUpdate, userRow, userName);
    }
    return userRow;
  }

  /**
   * Prepares the user object for update based on the received modification type.
   *
   * <p>The exact update operation is determined by the {@link Modification.ModificationType}.
   * Depending on the type (ADD, REPLACE, or DELETE), the attribute value is extracted or cleared
   * and delegated to the corresponding user attribute handler.
   *
   * @param modificationType           type of modification requested
   * @param attribute                  LDAP attribute being modified
   * @param attributeReceivedForUpdate resolved user attribute to be updated
   * @param userRow                    user object being prepared for update
   * @throws ServiceNowException in case of invalid attribute or modification type, multivalued
   *                             attribute, attributes which cannot be set/updated/kept
   *                             blank/deleted/invalid identity type value/invalid boolean string
   *                             value/if the attribute does not support direct setting/no role
   *                             added/username attribute received for update
   */
  private void prepareUserObjectForModification(
      final Modification.ModificationType modificationType, final Attribute attribute,
      final User attributeReceivedForUpdate, final UserRow userRow, final String userName)
      throws ServiceNowException, IOException {
    boolean isUserRolesAttribute = User.USER_ROLES.getAttributeName()
        .equalsIgnoreCase(attributeReceivedForUpdate.getAttributeName());
    switch (modificationType) {
      case DELETE: {
        if (isUserRolesAttribute) {
          userRoleRelationshipManager.processUserRoleDeletion(attribute, userName);
        } else {
          attributeReceivedForUpdate.apply(userRow, StringUtils.EMPTY, modificationType, attribute);
        }
        break;
      }
      case ADD:
      case REPLACE: {
        if (isUserRolesAttribute) {
          userRoleRelationshipManager.processUserRoleModification(
              modificationType, attribute, userName);
        } else {
          checkWhetherUserNameAttribute(attributeReceivedForUpdate);
          // for now user object is not having any attribute that can be multivalued
          Util.validateSingleValue(attribute);
          String attributeValue = attribute.getValues().get(0).trim();
          attributeReceivedForUpdate.apply(userRow, attributeValue, modificationType, attribute);
        }
        break;
      }
      default: {
        throw new ServiceNowException(
            String.format("invalid modification type received: %s", modificationType.name()));
      }
    }
  }

  private void checkWhetherUserNameAttribute(final User attributeReceivedForUpdate)
      throws ServiceNowException {
    boolean isUserNameAttribute = User.USER_NAME.getAttributeName()
        .equalsIgnoreCase(attributeReceivedForUpdate.getAttributeName());
    if (isUserNameAttribute) {
      throw new ServiceNowException("username attribute cannot be updated");
    }
  }

  /**
   * Invokes the ServiceNow API to update the user record.
   *
   * @param userRow user data prepared for update
   * @param sysId   unique identifier of the user
   * @throws IOException         if a communication error occurs while calling the API
   * @throws ServiceNowException In case api response is not ok/api response contains no user
   *                             entries/response is not containing the updated value
   */
  private void updateUser(final UserRow userRow, final String sysId)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getUpdateUserRequest(userRow, sysId));
    int responseCode = responseData.getResponseCode();
    log.debug("update user api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.OK.asInt()) {
      throw new ServiceNowException(
          String.format("update user api response message is: %s", apiResponse));
    }
  }
}
