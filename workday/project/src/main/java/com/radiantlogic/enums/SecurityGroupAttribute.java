package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.interfaces.ReportAttribute;
import com.radiantlogic.model.SecurityGroup;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * Enum representing supported Security Group attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link SecurityGroup}, enabling attribute-driven data extraction
 * without switch statements.
 */
@Getter
public enum SecurityGroupAttribute implements ReportAttribute<SecurityGroup> {

  WORKDAY_ID("WorkdayId", SecurityGroup::getWorkdayId),
  NAME("Name", SecurityGroup::getName),
  TYPE("Type", SecurityGroup::getType),
  SCOPE("Scope", SecurityGroup::getScope),
  IS_INACTIVE("IsInactive", SecurityGroup::getIsInactive);

  /**
   * The attribute name as used in the data model.
   */
  private final String attributeName;

  /**
   * Function to extract value from SecurityGroup.
   */
  private final Function<SecurityGroup, Object> getter;

  /**
   * Lookup map for case-insensitive attribute resolution.
   */
  private static final Map<String, SecurityGroupAttribute> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              attr -> attr.attributeName.toLowerCase(Locale.ROOT),
              attr -> attr));

  SecurityGroupAttribute(
      final String attributeName,
      final Function<SecurityGroup, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the value of the mapped attribute from the specified security group.
   *
   * @param group the security group from which the attribute value is extracted
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  @Override
  public Object extractValue(final SecurityGroup group) {
    return getter.apply(group);
  }

  /**
   * Returns the {@code SecurityGroupAttribute} enum constant associated with the specified
   * attribute name.
   *
   * <p>Attribute name matching is performed in a case-insensitive manner.</p>
   *
   * @param fieldName the attribute name to resolve
   * @return the matching {@code SecurityGroupAttribute} enum constant
   * @throws WorkdayException if no {@code SecurityGroupAttribute} constant exists for the
   *                          specified attribute name
   */
  public static SecurityGroupAttribute fromName(final String fieldName)
      throws WorkdayException {
    final SecurityGroupAttribute attribute = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (attribute == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return attribute;
  }
}
