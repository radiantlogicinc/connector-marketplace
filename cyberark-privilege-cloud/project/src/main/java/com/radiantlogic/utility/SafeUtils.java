package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.DATE_TIME_FORMATTER;
import static com.radiantlogic.utility.Constants.MICROSECONDS_TO_SECONDS_CONVERT_DIVISOR;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.ZONE;

import com.radiantlogic.enums.Safe;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This class will contain safe related utility methods.
 */
@Slf4j
public final class SafeUtils {

  private SafeUtils() {
  }

  /**
   * This method will convert fetched safes data into list of maps.
   *
   * @param safes        Fetched safes data
   * @param schema       Safe schema
   * @param targetSchema Safe target schema
   * @return List of maps of safes
   */
  public static List<Map<String, Object>> extractSafesData(final List<SafeRow> safes,
                                                           final Schema schema,
                                                           final ReadOnlyProperties targetSchema) {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Utils.getFieldNames(Utils.getSchemaObject(schema, Utils.getSchemaObjectName(targetSchema)));
    for (SafeRow safe : safes) {
      Map<String, Object> entry = populateSafeMap(safe, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  @SuppressWarnings(PMD_KEY)
  private static Map<String, Object> populateSafeMap(final SafeRow safe,
                                                     final List<String> fieldNames) {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      // below code has been used as an alternative to having a catch block with case block for
      // each field name, to ensure that fields which are part of schema, value only for those
      // should be returned
      Safe safeAttribute = Safe.fromName(fieldName);
      Object value = safeAttribute.extractValue(safe);
      if (value != null) {
        entry.put(safeAttribute.getAttributeName(), value);
      }
    }
    return entry;
  }

  /**
   * This method will format safe members membership expiry date.
   *
   * @param members List of safe members for whom membership expiry date to be formatted
   */
  public static void formatMembersMembershipExpirationDate(final List<SafeMember> members) {
    for (SafeMember member : members) {
      Long membershipExpirationDateEpoch = member.getMembershipExpirationDate();
      if (membershipExpirationDateEpoch != null) {
        member.setMembershipExpiry(Instant.ofEpochSecond(membershipExpirationDateEpoch).atZone(ZONE)
            .format(DATE_TIME_FORMATTER));
        // to avoid rendering epoch time as formatted time will be there
        member.setMembershipExpirationDate(null);
      }
    }
  }

  /**
   * This method will format safes created and modified dates.
   *
   * @param safes List if safes for whom dates to be set
   */
  public static void formatSafesDates(final List<SafeRow> safes) {
    for (SafeRow safe : safes) {
      safe.setCreated(
          Instant.ofEpochSecond(safe.getCreationTime()).atZone(ZONE).format(DATE_TIME_FORMATTER));
      safe.setModified(Instant.ofEpochSecond(safe.getLastModificationTime()
          / MICROSECONDS_TO_SECONDS_CONVERT_DIVISOR).atZone(ZONE).format(DATE_TIME_FORMATTER));
    }
  }
}