package com.radiantlogic.utility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.radiantlogic.enums.SupervisoryOrganization;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.model.SupervisoryOrganizationRow;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Utility class for Supervisory Organization-related helper methods.
 *
 * <p>This class provides static utility methods for working with supervisory organization
 * posting entities, requests, and related transformations.</p>
 */

public final class SupervisoryOrganizationUtil {

  private SupervisoryOrganizationUtil() {
  }

  /**
   * Extracts Supervisory Organization data from Workday response records and maps it to schema
   * object representations.
   *
   * <p>Each Supervisory Organization record is transformed into a map of
   * attribute values based on the provided schema definition and target schema properties.
   *
   * @param supervisoryOrganizations the Supervisory Organization records retrieved from Workday
   * @param schema                   the schema definition used to map Supervisory Organization
   *                                 attributes
   * @param targetSchema             the target schema properties used during data extraction and
   *                                 transformation
   * @return a list of mapped Supervisory Organization data objects represented as attribute-value
   *          maps
   * @throws WorkdayException if an error occurs while extracting or mapping Supervisory
   *                          Organization data
   */
  public static List<Map<String, Object>> extractSupervisoryOrganizationsData(
      final @NonNull List<SupervisoryOrganizationRow> supervisoryOrganizations,
      final @NonNull Schema schema,
      final @NonNull ReadOnlyProperties targetSchema) throws WorkdayException {
    final List<String> fieldNames =
        Util.getFieldNames(
            Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));
    final ImmutableList.Builder<Map<String, Object>> searchResultData =
        ImmutableList.builder();
    for (SupervisoryOrganizationRow supervisoryOrganization : supervisoryOrganizations) {
      Map<String, Object> entry =
          populateSupervisoryOrganizationMap(supervisoryOrganization, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData.build();
  }

  private static Map<String, Object> populateSupervisoryOrganizationMap(
      final SupervisoryOrganizationRow supervisoryOrganization,
      final List<String> fieldNames)
      throws WorkdayException {
    final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (String fieldName : fieldNames) {
      SupervisoryOrganization supervisoryOrganizationAttribute =
          SupervisoryOrganization.fromName(fieldName);
      Object value = supervisoryOrganizationAttribute.extractValue(supervisoryOrganization);
      if (Util.isValidValue(value)) {
        builder.put(supervisoryOrganizationAttribute.getAttributeName(), value);
      }
    }
    return builder.build();
  }

}
