package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.ASTERISK_KEY;
import static com.radiantlogic.utility.Constants.ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE;
import static com.radiantlogic.utility.Constants.GET_USERS_PAYLOAD_FOR_FILTERS_FOR_EXACT_VALUE;
import static com.radiantlogic.utility.Constants.GET_USERS_PAYLOAD_FOR_FILTERS_FOR_LIKE_COMMAND;
import static com.radiantlogic.utility.Constants.GET_USERS_PAYLOAD_FOR_FILTERS_WITH_NUMERIC_COLUMNS_FOR_EXACT_VALUE;
import static com.radiantlogic.utility.Constants.GET_USERS_PAYLOAD_FOR_FILTERS_WITH_NUMERIC_COLUMNS_FOR_LIKE_COMMAND;
import static com.radiantlogic.utility.Constants.NOT_OPERATOR_KEY;
import static com.radiantlogic.utility.Constants.NULL_KEY;
import static com.radiantlogic.utility.Constants.PERCENTAGE_KEY;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.RISK_LEVEL_RANK_KEY_NAME;
import static com.radiantlogic.utility.Constants.USER_MAIL_SERIALIZED_NAME;
import static com.radiantlogic.utility.Constants.USER_NAME_SERIALIZED_NAME;
import static com.radiantlogic.utility.Constants.USER_SOURCE_SERIALIZED_NAME;

import com.radiantlogic.enums.User;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.Modification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class will contain utility functions used by connector.
 */
@Slf4j
@SuppressWarnings(PMD_KEY)
public final class UserUtils {

  private UserUtils() {
  }

  /**
   * This method will set additional user object attributes.
   *
   * @param userRecordRow Row which will be updated
   */
  public static void setAdditionalCreateUserAttributes(final RecordRow userRecordRow) {
    if (StringUtils.isBlank(userRecordRow.getMail())) {
      userRecordRow.setSendEmailInvite(false);
      if (StringUtils.isNotBlank(userRecordRow.getMobile())) {
        userRecordRow.setSendSmsInvite(true);
      }
    }
  }

  /**
   * This method will populate user object attributes.
   *
   * @param attributeReceivedForCreate User attribute to be set
   * @param uniqueKey                  RDN value
   * @param attribute                  Attribute
   * @param userRecordRow              Row which will be updated
   * @throws CyberArkPrivilegeCloudException In case rdn & primary key's value are not same/invalid
   *                                         attribute received for create
   */
  public static void populateCreateUserObject(final User attributeReceivedForCreate,
                                              final String uniqueKey, final Attribute attribute,
                                              final RecordRow userRecordRow)
      throws CyberArkPrivilegeCloudException {
    String attributeValue = attribute.getValues().get(0);
    switch (attributeReceivedForCreate) {
      case NAME:
      case USER: {
        if (!uniqueKey.equals(attributeValue)) {
          throw new CyberArkPrivilegeCloudException("Name and rdn value has to be same");
        }
        userRecordRow.setName(attributeValue);
        break;
      }
      case DISPLAY_NAME: {
        userRecordRow.setDisplayName(attributeValue);
        break;
      }
      case MAIL: {
        userRecordRow.setMail(attributeValue);
        break;
      }
      case MOBILE: {
        userRecordRow.setMobile(attributeValue);
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("invalid attribute received for create user:%s", attribute.getName()));
      }
    }
  }

  /**
   * This method will set user object attributes.
   *
   * @param attributeReceivedForUpdate User attribute to be updated
   * @param userRecordRow              Row where values will be updated
   * @param attributeValue             Value to be updated
   * @param modificationType           ModificationType like DELETE
   * @param attribute                  Attribute
   * @throws CyberArkPrivilegeCloudException In case user display name is intended to be
   *                                         deleted/invalid attribute received for update
   */
  public static void updateUserObject(final User attributeReceivedForUpdate,
                                      final RecordRow userRecordRow, final String attributeValue,
                                      final Modification.ModificationType modificationType,
                                      final Attribute attribute)
      throws CyberArkPrivilegeCloudException {
    switch (attributeReceivedForUpdate) {
      case DISPLAY_NAME: {
        if (modificationType.equals(Modification.ModificationType.DELETE)) {
          throw new CyberArkPrivilegeCloudException(
              String.format("%s attribute cannot be deleted", attribute.getName()));
        }
        userRecordRow.setDisplayName(attributeValue);
        break;
      }
      case MOBILE: {
        userRecordRow.setMobile(attributeValue);
        break;
      }
      case MAIL: {
        userRecordRow.setMail(attributeValue);
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("'%s' attribute cannot be updated/deleted:", attribute.getName()));
      }
    }
  }

  /**
   * This method will update user object attribute's value based on modification type.
   *
   * @param modificationType           Modification Type
   * @param attribute                  Attribute containing value to be updated
   * @param attributeReceivedForUpdate User attribute to be updated
   * @param userRecordRow              Row that will store updated values
   * @throws CyberArkPrivilegeCloudException In case values size is greater than one/invalid
   *                                         attribute/modification type
   */
  public static void processUserUpdateBasedOnModificationType(
      final Modification.ModificationType modificationType, final Attribute attribute,
      final User attributeReceivedForUpdate, final RecordRow userRecordRow)
      throws CyberArkPrivilegeCloudException {
    switch (modificationType) {
      case DELETE: {
        updateUserObject(attributeReceivedForUpdate, userRecordRow, StringUtils.EMPTY,
            modificationType, attribute);
        break;
      }
      case ADD:
      case REPLACE: {
        String attributeValue;
        // for now user object is not having any attribute that can be multivalued
        if (attribute.getValues().size() == 1 && StringUtils.isNotBlank(
            attribute.getValues().get(0))) {
          attributeValue = attribute.getValues().get(0);
        } else {
          throw new CyberArkPrivilegeCloudException(
              String.format("only single/non blank value for attribute: %s is supported",
                  attribute.getName()));
        }
        updateUserObject(attributeReceivedForUpdate, userRecordRow, attributeValue,
            modificationType, attribute);
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("invalid modification type received: %s", modificationType.name()));
      }
    }
  }

  /**
   * This method will populate user object for update.
   *
   * @param ldapModifyRequest LdapModifyRequest containing update details
   * @param schemaObject      SchemaObject
   * @param users             User to be updated
   * @return Row containing values to be updated
   * @throws CyberArkPrivilegeCloudException In case values size is greater than one/invalid
   *                                         attribute/modification type
   */
  @NonNull
  public static RecordRow populateUserObjectForUpdate(final LdapModifyRequest ldapModifyRequest,
                                                      final SchemaObject schemaObject,
                                                      final List<Record> users)
      throws CyberArkPrivilegeCloudException {
    RecordRow userRecordRow = new RecordRow();
    userRecordRow.setId(users.get(0).getRecordRow().getId());
    for (Modification modification : ldapModifyRequest.getModifications()) {
      Attribute attribute = modification.getAttribute();
      Modification.ModificationType modificationType = modification.getModificationType();
      log.debug(ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE, attribute.getName(),
          attribute.getValues(), modificationType.name());
      String attributeName = Utils.getAttributeName(schemaObject, attribute.getName());
      User attributeReceivedForUpdate = getMatchingAttributeForUserUpdate(attributeName);
      processUserUpdateBasedOnModificationType(modificationType, attribute,
          attributeReceivedForUpdate, userRecordRow);
    }
    return userRecordRow;
  }

  private static User getMatchingAttributeForUserUpdate(final String attributeName) {
    return Arrays.stream(User.values()).filter(
        x -> StringUtils.isNotBlank(attributeName) && x.getAttributeName()
            .equalsIgnoreCase(attributeName)).findFirst().orElseThrow(IllegalStateException::new);
  }

  /**
   * This method will populate user fields for get user(s) operation.
   *
   * @param user          Fetched user object containing user data
   * @param fieldName     Attribute name as part of schema
   * @param userAttribute User attribute to be set
   * @param entry         Map that will store key values
   * @throws CyberArkPrivilegeCloudException In case invalid attribute is received
   */
  public static void populateUserFields(final Record user, final String fieldName,
                                        final User userAttribute, final Map<String, Object> entry)
      throws CyberArkPrivilegeCloudException {
    switch (userAttribute) {
      case NAME: {
        entry.put(User.NAME.getAttributeName(), user.getRecordRow().getUsername());
        break;
      }
      case DISPLAY_NAME: {
        entry.put(User.DISPLAY_NAME.getAttributeName(), user.getRecordRow().getDisplayName());
        break;
      }
      case ID: {
        entry.put(User.ID.getAttributeName(), user.getRecordRow().getId());
        break;
      }
      case RISK_LEVEL_RANK: {
        entry.put(User.RISK_LEVEL_RANK.getAttributeName(), user.getRecordRow().getRiskLevelRank());
        break;
      }
      case STATUS: {
        entry.put(User.STATUS.getAttributeName(), user.getRecordRow().getStatus());
        break;
      }
      case MOBILE: {
        entry.put(User.MOBILE.getAttributeName(),
            StringUtils.isNotBlank(user.getRecordRow().getMobile()) ?
                user.getRecordRow().getMobile() : null);
        break;
      }
      case MAIL: {
        entry.put(User.MAIL.getAttributeName(),
            StringUtils.isNotBlank(user.getRecordRow().getEmail()) ?
                user.getRecordRow().getEmail() : null);
        break;
      }
      case SOURCE: {
        entry.put(User.SOURCE.getAttributeName(), user.getRecordRow().getSource());
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("invalid attribute name received:%s", fieldName));
      }
    }
  }

  /**
   * This method will populate user map.
   *
   * @param user       User object containing fetched data
   * @param fieldNames Field names as per schema
   * @return User record map
   * @throws CyberArkPrivilegeCloudException In case of invalid attribute name as part of field
   *                                         names
   */
  public static Map<String, Object> populateUserMap(final Record user,
                                                    final List<String> fieldNames)
      throws CyberArkPrivilegeCloudException {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      User userAttribute =
          Arrays.stream(User.values()).filter(x -> x.getAttributeName().equalsIgnoreCase(fieldName))
              .findFirst().orElseThrow(IllegalStateException::new);
      populateUserFields(user, fieldName, userAttribute, entry);
    }
    return entry;
  }

  /**
   * This method will covert fetched users data into list of maps.
   *
   * @param recordList   Fetched users data
   * @param schema       Request schema
   * @param targetSchema Request Schema name
   * @return List of maps of users data
   * @throws CyberArkPrivilegeCloudException In case of invalid attribute while populating data
   */
  public static List<Map<String, Object>> extractUserData(final List<Record> recordList,
                                                          final Schema schema,
                                                          final ReadOnlyProperties targetSchema)
      throws CyberArkPrivilegeCloudException {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Utils.getFieldNames(Utils.getSchemaObject(schema, Utils.getSchemaObjectName(targetSchema)));
    for (Record user : recordList) {
      Map<String, Object> entry = populateUserMap(user, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  /**
   * This method will provide matching User attribute for the provided attribute.
   *
   * @param attributeName Provided attribute name
   * @return Matching User Attribute
   */
  @Nullable
  public static User getMatchingUserAttribute(final String attributeName) {
    return Arrays.stream(User.values()).filter(
        x -> StringUtils.isNotBlank(attributeName) && x.getAttributeName()
            .equalsIgnoreCase(attributeName)).findFirst().orElse(null);
  }

  /**
   * Method to populate filter map for users.
   *
   * @param filter          Search filter containing filters received
   * @param matchingFilters Map to be populated
   */
  public static void populateFilterMapForUser(final SearchFilter filter,
                                              final Map<String, List<String>> matchingFilters) {
    String attributeName = filter.getAttribute();
    String attributeValue = filter.getAssertionValue().orElse(null);
    SearchFilter.Type searchFilterType = filter.getType();
    if (StringUtils.isBlank(attributeValue) || NULL_KEY.equalsIgnoreCase(attributeValue.trim())) {
      log.info("either attribute value is blank or having value 'null'");
      return;
    }
    User matchingAttribute = UserUtils.getMatchingUserAttribute(attributeName);
    if (matchingAttribute != null && !NOT_OPERATOR_KEY.equalsIgnoreCase(searchFilterType.name())) {
      switch (matchingAttribute) {
        case SOURCE: {
          matchingFilters.computeIfAbsent(USER_SOURCE_SERIALIZED_NAME, values -> new ArrayList<>())
              .add(attributeValue.toLowerCase(Locale.ROOT));
          break;
        }
        case NAME: {
          matchingFilters.computeIfAbsent(USER_NAME_SERIALIZED_NAME, values -> new ArrayList<>())
              .add(attributeValue.toLowerCase(Locale.ROOT));
          break;
        }
        case MAIL: {
          matchingFilters.computeIfAbsent(USER_MAIL_SERIALIZED_NAME, values -> new ArrayList<>())
              .add(attributeValue.toLowerCase(Locale.ROOT));
          break;
        }
        case DISPLAY_NAME:
        case ID:
        case RISK_LEVEL_RANK:
        case STATUS:
        case MOBILE: {
          matchingFilters.computeIfAbsent(
                  matchingAttribute.getAttributeName().toLowerCase(Locale.ROOT),
                  values -> new ArrayList<>())
              .add(attributeValue.toLowerCase(Locale.ROOT));
          break;
        }
      }
    } else {
      log.info("no matching filter attribute found/NOT operator not allowed");
    }
  }

  /**
   * Method to get users payload json string based on filter value
   *
   * @param pageNumber Page Number
   * @param pageSize   Page Size
   * @param filters    Map containing filter
   * @return Payload json string
   */
  @NotNull
  public static String getPayloadWithFilters(int pageNumber, int pageSize,
                                             final Map<String, String> filters) {
    String keyName = filters.keySet().iterator().next();
    String value = filters.get(keyName);
    boolean useLike = value.contains(ASTERISK_KEY);
    if (useLike) {
      value = value.replace(ASTERISK_KEY, PERCENTAGE_KEY);
    }
    boolean isNumericColumn =
        RISK_LEVEL_RANK_KEY_NAME.equalsIgnoreCase(keyName);
    String template;
    if (isNumericColumn) {
      template = useLike
          ? GET_USERS_PAYLOAD_FOR_FILTERS_WITH_NUMERIC_COLUMNS_FOR_LIKE_COMMAND
          : GET_USERS_PAYLOAD_FOR_FILTERS_WITH_NUMERIC_COLUMNS_FOR_EXACT_VALUE;
    } else {
      template = useLike
          ? GET_USERS_PAYLOAD_FOR_FILTERS_FOR_LIKE_COMMAND
          : GET_USERS_PAYLOAD_FOR_FILTERS_FOR_EXACT_VALUE;
    }
    return String.format(
        template,
        keyName,
        value,
        pageNumber,
        pageSize,
        pageSize
    );
  }
}
