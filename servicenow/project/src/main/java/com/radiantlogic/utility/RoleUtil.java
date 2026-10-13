package com.radiantlogic.utility;

import com.radiantlogic.enums.Role;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.RoleRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Class containing role related util methods.
 */
@Slf4j
public final class RoleUtil {

  private RoleUtil() {
  }

  /**
   * Converts fetched roles data into list of maps.
   *
   * @param roles        Fetched roles data
   * @param schema       Role schema
   * @param targetSchema Role target schema
   * @return List of maps of roles
   * @throws ServiceNowException In case invalid field name is received
   */
  public static List<Map<String, Object>> extractRolesData(final List<RoleRow> roles,
                                                           final Schema schema,
                                                           final ReadOnlyProperties targetSchema)
      throws ServiceNowException {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Util.getFieldNames(Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));
    for (RoleRow role : roles) {
      Map<String, Object> entry = populateRoleMap(role, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  @SuppressWarnings("PMD.UseConcurrentHashMap")
  private static Map<String, Object> populateRoleMap(final RoleRow role,
                                                     final List<String> fieldNames)
      throws ServiceNowException {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      Role roleAttribute = Role.fromName(fieldName);
      Object value = roleAttribute.extractValue(role);
      if (Util.isValidValue(value)) {
        entry.put(roleAttribute.getAttributeName(), value);
      }
    }
    return entry;
  }

  /**
   * Checks whether child roles to be fetched.
   *
   * @param schema           Schema object
   * @param schemaObjectName Schema object name
   * @return Whether child roles to be fetched
   */
  public static boolean checkWhetherFetchChildRoles(final Schema schema,
                                                    final String schemaObjectName) {
    List<String> fieldNames =
        Util.getFieldNames(Util.getSchemaObject(schema, schemaObjectName));
    String childRolesAttribute =
        Role.CHILD_ROLES.getAttributeName();
    return fieldNames.stream()
        .anyMatch(fieldName -> fieldName.equalsIgnoreCase(childRolesAttribute));
  }
}
