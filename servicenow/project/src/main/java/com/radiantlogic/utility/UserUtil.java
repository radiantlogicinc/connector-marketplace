package com.radiantlogic.utility;

import com.radiantlogic.enums.User;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.UserRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Class containing user related util methods.
 */
@Slf4j
public final class UserUtil {

  private UserUtil() {
  }

  /**
   * Method to convert fetched users data into list of maps.
   *
   * @param users        Fetched users data
   * @param schema       User schema
   * @param targetSchema User target schema
   * @return List of maps of users
   * @throws ServiceNowException In case invalid field name is received
   */
  public static List<Map<String, Object>> extractUsersData(final List<UserRow> users,
                                                           final Schema schema,
                                                           final ReadOnlyProperties targetSchema)
      throws ServiceNowException {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Util.getFieldNames(Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));
    for (UserRow user : users) {
      Map<String, Object> entry = populateUserMap(user, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  @SuppressWarnings("PMD.UseConcurrentHashMap")
  private static Map<String, Object> populateUserMap(final UserRow user,
                                                     final List<String> fieldNames)
      throws ServiceNowException {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      User userAttribute = User.fromName(fieldName);
      Object value = userAttribute.extractValue(user);
      if (Util.isValidValue(value)) {
        entry.put(userAttribute.getAttributeName(), value);
      }
    }
    return entry;
  }

  /**
   * Checks whether user roles to be fetched.
   *
   * @param schema       Schema object
   * @param targetSchema Target schema properties
   * @return Whether user roles to be fetched
   */
  public static boolean isGetUserRoles(final Schema schema,
                                       final ReadOnlyProperties targetSchema) {
    String schemaObjectName = Util.getSchemaObjectName(targetSchema);
    List<String> fieldNames =
        Util.getFieldNames(Util.getSchemaObject(schema, schemaObjectName));
    String userRolesAttribute =
        User.USER_ROLES.getAttributeName();
    return fieldNames.stream()
        .anyMatch(fieldName -> fieldName.equalsIgnoreCase(userRolesAttribute));
  }
}
