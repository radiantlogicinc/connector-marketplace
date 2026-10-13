package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.interfaces.ReportAttribute;
import com.radiantlogic.model.SecurityGroupMembership;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * Enum representing supported Security Group Membership attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link SecurityGroupMembership}, enabling attribute-driven data
 * extraction without switch statements.
 */
@Getter
public enum SecurityGroupMembershipAttribute implements ReportAttribute<SecurityGroupMembership> {

  GROUP_NAME("Name", SecurityGroupMembership::getName),
  WORKDAY_ID("WorkdayId", SecurityGroupMembership::getWorkdayId),
  MEMBERS("Members", SecurityGroupMembership::getMembersList);

  /**
   * The attribute name as used in the data model.
   */
  private final String attributeName;

  /**
   * Function to extract value from SecurityGroupMembership.
   */
  private final Function<SecurityGroupMembership, Object> getter;

  /**
   * Lookup map for case-insensitive attribute resolution.
   */
  private static final Map<String, SecurityGroupMembershipAttribute> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              attr -> attr.attributeName.toLowerCase(Locale.ROOT),
              attr -> attr));

  SecurityGroupMembershipAttribute(
      final String attributeName,
      final Function<SecurityGroupMembership, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the value of the mapped attribute from the specified security group membership.
   *
   * @param membership the security group membership from which the attribute value is extracted
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  @Override
  public Object extractValue(final SecurityGroupMembership membership) {
    return getter.apply(membership);
  }

  /**
   * Returns the {@code SecurityGroupMembershipAttribute} enum constant associated with the
   * specified attribute name.
   *
   * <p>Attribute name matching is performed in a case-insensitive manner.</p>
   *
   * @param fieldName the attribute name to resolve
   * @return the matching {@code SecurityGroupMembershipAttribute} enum constant
   * @throws WorkdayException if no {@code SecurityGroupMembershipAttribute} constant exists for the
   *                          specified attribute name
   */
  public static SecurityGroupMembershipAttribute fromName(final String fieldName)
      throws WorkdayException {
    final SecurityGroupMembershipAttribute attribute =
        LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (attribute == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return attribute;
  }
}
