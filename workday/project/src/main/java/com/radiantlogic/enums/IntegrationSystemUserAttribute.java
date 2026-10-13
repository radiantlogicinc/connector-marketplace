package com.radiantlogic.enums;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.interfaces.ReportAttribute;
import com.radiantlogic.model.IntegrationSystemUser;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Enum representing supported Integration System User attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link IntegrationSystemUser}, enabling attribute-driven data
 * extraction without switch statements.
 */
@Getter
public enum IntegrationSystemUserAttribute implements ReportAttribute<IntegrationSystemUser> {

  DESCRIPTION("Description", IntegrationSystemUser::getDescription),
  IS_DISABLED("IsDisabled", IntegrationSystemUser::getIsDisabled),
  USER_NAME("UserName", IntegrationSystemUser::getUserName),
  WORKDAY_ID("WorkdayId", IntegrationSystemUser::getWorkdayId);

  /**
   * Name of the external attribute that this mapping represents. Used to identify and resolve
   * values from external data sources.
   */
  private final String attributeName;

  /**
   * Function that extracts the corresponding attribute value from an {@link IntegrationSystemUser}
   * instance.
   */
  private final Function<IntegrationSystemUser, Object> getter;

  /**
   * Immutable lookup map that associates normalized attribute names with their corresponding
   * {@code IntegrationSystemUserAttribute} enum constants.
   *
   * <p>The keys are stored in lowercase using {@link Locale#ROOT}
   * to support case-insensitive attribute resolution.</p>
   */
  private static final Map<String, IntegrationSystemUserAttribute> LOOKUP =
      Collections.unmodifiableMap(
          Arrays.stream(values()).collect(
              Collectors.toMap(
                  attr -> attr.attributeName.toLowerCase(Locale.ROOT),
                  attr -> attr)));

  IntegrationSystemUserAttribute(
      final String attributeName,
      final Function<IntegrationSystemUser, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the value of the mapped attribute from the specified integration system user.
   *
   * @param user the integration system user from which the attribute value is extracted
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  @Override
  public Object extractValue(final IntegrationSystemUser user) {
    return getter.apply(user);
  }

  /**
   * Returns the {@code IntegrationSystemUserAttribute} enum constant associated with the specified
   * attribute name.
   *
   * <p>Attribute name matching is performed in a case-insensitive manner.</p>
   *
   * @param fieldName the attribute name to resolve
   * @return the matching {@code IntegrationSystemUserAttribute} enum constant
   * @throws WorkdayException if no {@code IntegrationSystemUserAttribute} constant exists for the
   *                          specified attribute name
   */
  public static IntegrationSystemUserAttribute fromName(final String fieldName)
      throws WorkdayException {
    IntegrationSystemUserAttribute attribute = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (attribute == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return attribute;
  }
}
