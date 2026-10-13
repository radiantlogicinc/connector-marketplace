package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.model.SupervisoryOrganizationRow;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * Enum representing supported supervisory organization attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link SupervisoryOrganizationRow}, enabling attribute-driven data
 * extraction without switch statements.
 */
@Getter
public enum SupervisoryOrganization {

  SUPERVISORYORGANIZATIONID("SupervisoryOrganizationId",
      SupervisoryOrganizationRow::getSupervisoryOrganizationId),
  DESCRIPTOR("Descriptor", SupervisoryOrganizationRow::getDescriptor),
  ORGANIZATIONREFERENCEID("OrganizationReferenceId",
      SupervisoryOrganizationRow::getOrganizationReferenceId),
  INACTIVE("Inactive", SupervisoryOrganizationRow::isInactive),
  MANAGERS("Managers", SupervisoryOrganizationRow::getManagers),
  NAME("Name", SupervisoryOrganizationRow::getName),
  ORGANIZATIONMEMBER("OrganizationMember", SupervisoryOrganizationRow::getOrganizationMember);

  /**
   * External attribute name used for lookup and mapping to enum constants.
   */
  private final String attributeName;

  /**
   * Function used to extract the attribute value from a {@link SupervisoryOrganizationRow}.
   */
  private final Function<SupervisoryOrganizationRow, Object> getter;

  /**
   * Lookup map that resolves attribute names (case-insensitive) to enum constants.
   */
  private static final Map<String, SupervisoryOrganization> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              supervisoryOrganization -> supervisoryOrganization.attributeName.toLowerCase(
                  Locale.ROOT),
              supervisoryOrganization -> supervisoryOrganization));

  SupervisoryOrganization(final String attributeName,
                          final Function<SupervisoryOrganizationRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the attribute value from the given SupervisoryOrganizationRow.
   *
   * @param supervisoryOrganization Supervisory Organization Row instance
   * @return Extracted value
   */
  public Object extractValue(final SupervisoryOrganizationRow supervisoryOrganization) {
    return getter.apply(supervisoryOrganization);
  }

  /**
   * Resolves enum constant from attribute name.
   *
   * @param fieldName Attribute name
   * @return Matching SupervisoryOrganization enum constant
   * @throws WorkdayException If the attribute name is invalid
   */
  public static SupervisoryOrganization fromName(final String fieldName) throws WorkdayException {
    SupervisoryOrganization supervisoryOrganization =
        LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (supervisoryOrganization == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return supervisoryOrganization;
  }

}
