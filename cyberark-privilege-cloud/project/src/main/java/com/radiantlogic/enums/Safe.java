package com.radiantlogic.enums;

import com.radiantlogic.utility.SafeCreator;
import com.radiantlogic.utility.SafeRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * This enum will contain safe entity related attributes. Data source should have below as part of
 * attribute names.
 */
@Getter
public enum Safe {
  NAME("Name", SafeRow::getSafeName),
  NUMBER("Number", SafeRow::getSafeNumber),
  DESCRIPTION("Description", SafeRow::getDescription),
  MEMBERS("Members", SafeRow::getMembers),
  LOCATION("Location", SafeRow::getLocation),
  MANAGING_CPM("ManagingCpm", SafeRow::getManagingCpm),
  IS_EXPIRED_MEMBER("IsExpiredMember", SafeRow::getIsExpiredMember),
  CREATED("Created", SafeRow::getCreated),
  MODIFIED("Modified", SafeRow::getModified),
  CREATOR(
      "Creator", safeRow -> Optional.ofNullable(safeRow.getSafeCreator())
      .map(SafeCreator::getName).orElse(null)),
  IS_OLAC_ENABLED("IsOlacEnabled", SafeRow::getIsOlacEnabled),
  NUMBER_OF_VERSIONS_RETENTION("NumberOfVersionsRetention", SafeRow::getNumberOfVersionsRetention),
  NUMBER_OF_DAYS_RETENTION("NumberOfDaysRetention", SafeRow::getNumberOfDaysRetention),
  IS_AUTO_PURGE_ENABLED("IsAutoPurgeEnabled", SafeRow::getIsAutoPurgeEnabled);

  /**
   * This field will store attribute name.
   */
  private final String attributeName;
  /**
   * This field will extract attribute's value.
   */
  private final Function<SafeRow, Object> getter;
  /**
   * Constant to define an immutable map containing field names and corresponding Safe object.
   */
  public static final Map<String, Safe> LOOKUP =
      Collections.unmodifiableMap(Arrays.stream(values())
          .collect(Collectors.toMap(safe -> safe.attributeName.toLowerCase(Locale.ROOT)
              , safe -> safe)));

  Safe(final String attributeName, final Function<SafeRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * This method will extract safe's particular field's/attribute's value.
   *
   * @param safe Safe object containing value to be extracted
   * @return Extracted value
   */
  public Object extractValue(final SafeRow safe) {
    return getter.apply(safe);
  }

  /**
   * This method will check whether received fieldName is valid or not.
   *
   * @param fieldName Field name to be validated
   * @return Matched Safe object
   */
  public static Safe fromName(final String fieldName) {
    Safe safe = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (safe == null) {
      throw new IllegalStateException(String.format("Invalid attribute name: %s", fieldName));
    }
    return safe;
  }
}