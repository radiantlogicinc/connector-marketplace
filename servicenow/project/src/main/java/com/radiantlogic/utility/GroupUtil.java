package com.radiantlogic.utility;

import com.radiantlogic.enums.Group;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.GroupRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Class containing group related util methods.
 */
@Slf4j
public final class GroupUtil {

  private GroupUtil() {
  }

  /**
   * Converts fetched groups data into list of maps.
   *
   * @param groups       Fetched groups data
   * @param schema       Group schema
   * @param targetSchema Group target schema
   * @return List of maps of groups
   * @throws ServiceNowException In case invalid field name is received
   */
  public static List<Map<String, Object>> extractGroupsData(final List<GroupRow> groups,
                                                            final Schema schema,
                                                            final ReadOnlyProperties targetSchema)
      throws ServiceNowException {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Util.getFieldNames(Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));
    for (GroupRow group : groups) {
      Map<String, Object> entry = populateGroupMap(group, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  @SuppressWarnings("PMD.UseConcurrentHashMap")
  private static Map<String, Object> populateGroupMap(final GroupRow group,
                                                      final List<String> fieldNames)
      throws ServiceNowException {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      Group groupAttribute = Group.fromName(fieldName);
      Object value = groupAttribute.extractValue(group);
      if (Util.isValidValue(value)) {
        entry.put(groupAttribute.getAttributeName(), value);
      }
    }
    return entry;
  }

  /**
   * Checks whether group users to be fetched.
   *
   * @param schema       Schema object
   * @param targetSchema Target schema properties
   * @return boolean whether group users to be fetched
   */
  public static boolean isGetGroupUsers(final Schema schema,
                                        final ReadOnlyProperties targetSchema) {
    String schemaObjectName = Util.getSchemaObjectName(targetSchema);
    List<String> fieldNames = Util.getFieldNames(
        Util.getSchemaObject(schema, schemaObjectName));
    return fieldNames.stream()
        .map(fieldName -> fieldName.toLowerCase(Locale.ROOT))
        .anyMatch(Group.GROUP_USERS.getAttributeName().toLowerCase(Locale.ROOT)::equals);
  }

  /**
   * Checks whether group roles field is present in schema.
   *
   * @param schema       Schema object
   * @param targetSchema Target schema object
   * @return boolean whether group roles field is present
   */
  public static boolean isGetGroupRoles(final Schema schema,
                                        final ReadOnlyProperties targetSchema) {
    String schemaObjectName = Util.getSchemaObjectName(targetSchema);
    List<String> fieldNames = Util.getFieldNames(
        Util.getSchemaObject(schema, schemaObjectName));
    return fieldNames.stream()
        .map(fieldName -> fieldName.toLowerCase(Locale.ROOT))
        .anyMatch(Group.GROUP_ROLES.getAttributeName().toLowerCase(Locale.ROOT)::equals);
  }
}
