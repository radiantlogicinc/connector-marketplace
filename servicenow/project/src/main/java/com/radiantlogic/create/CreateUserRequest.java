package com.radiantlogic.create;

import static com.radiantlogic.utility.Constants.ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.USER_ENTITY_KEY;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.enums.User;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.iddm.ldap.RDN;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to handle create user request.
 */
@Slf4j
@AllArgsConstructor
public final class CreateUserRequest {

  /**
   * Builds ServiceNow API requests for user operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Executes the constructed API requests.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Target schema object configuration used to determine the object type.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * Schema containing definitions of supported objects and their attributes.
   */
  private final Schema schema;

  /**
   * Handles LDAP add request to create a user in ServiceNow.
   *
   * @param ldapAddRequest LDAP add request containing user attributes
   * @return {@link LdapResponse} indicating the result of the operation
   */
  public LdapResponse<Void> create(final LdapAddRequest ldapAddRequest) {
    try {
      String uniqueKey = getUniqueKey(ldapAddRequest);
      SchemaObject schemaObject =
          Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchemaObjects));
      UserRow userRow = createUserObject(ldapAddRequest, schemaObject, uniqueKey);
      if (StringUtils.isBlank(userRow.getUsername())) {
        log.error("username field not found");
        return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
      }
      createUser(userRow);
      return new LdapResponse<>(LdapResultCode.SUCCESS);
    } catch (ServiceNowException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
      return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
    }
  }

  private void createUser(final UserRow userRow)
      throws IOException, ServiceNowException {
    ResponseData responseData =
        requestExecutor.execute(requestBuilder.getCreateUserRequest(userRow));
    int responseCode = responseData.getResponseCode();
    log.debug("create user api response code with message is: {} {}",
        responseCode, responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode != HttpStatus.CREATED.asInt()) {
      throw new ServiceNowException(
          String.format("create user api response message is: %s", apiResponse));
    }
  }

  @NonNull
  private UserRow createUserObject(final LdapAddRequest ldapAddRequest,
                                   final SchemaObject schemaObject, final String uniqueKey)
      throws ServiceNowException {
    UserRow userRow = new UserRow();
    List<Attribute> attributes = ldapAddRequest.getAttributes();
    for (Attribute attribute : attributes) {
      log.debug(ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE, attribute.getName(),
          attribute.getValues(), Modification.ModificationType.ADD);
      String attributeName = Util.getAttributeName(schemaObject, attribute.getName());
      Optional<User> attributeReceivedForCreate = User.fromNameOptional(attributeName);
      if (attributeReceivedForCreate.isPresent()) {
        Util.validateSingleValue(attribute);
        String attributeValue = attribute.getValues().get(0).trim();
        User matchingUserAttribute = attributeReceivedForCreate.get();
        if (matchingUserAttribute.equals(User.USER_NAME)) {
          validateUsernameMatchesUniqueKey(attributeValue, uniqueKey);
        }
        matchingUserAttribute.apply(userRow, attributeValue, Modification.ModificationType.ADD,
            attribute);
      }
    }
    return userRow;
  }

  private static void validateUsernameMatchesUniqueKey(final String attributeValue,
                                                       final String uniqueKey)
      throws ServiceNowException {
    if (!StringUtils.equalsIgnoreCase(attributeValue, uniqueKey)) {
      throw new ServiceNowException(
          "Username value must match the rdn value");
    }
  }

  private static String getUniqueKey(final LdapAddRequest ldapAddRequest)
      throws ServiceNowException {
    for (RDN component : ldapAddRequest.getDn().getRDNs()) {
      List<Attribute> attributes = component.getAttributes();
      for (Attribute attribute : attributes) {
        if (USER_ENTITY_KEY.equalsIgnoreCase(attribute.getName().toLowerCase(Locale.ROOT))) {
          Util.validateSingleValue(attribute);
          return attribute.getValues().get(0).trim();
        }
      }
    }
    throw new ServiceNowException("request is not for user entity");
  }
}
