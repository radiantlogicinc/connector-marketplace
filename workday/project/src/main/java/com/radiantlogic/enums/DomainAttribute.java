package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.interfaces.ReportAttribute;
import com.radiantlogic.model.Domain;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * Enum representing supported domain attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link Domain}, enabling attribute-driven data extraction without
 * switch statements.
 */
@Getter
public enum DomainAttribute implements ReportAttribute<Domain> {

  WORKDAY_ID("WorkdayId", Domain::getWorkdayId),
  NAME("Name", Domain::getName),
  DESCRIPTION("Description", Domain::getDescription),
  FUNCTIONAL_AREAS("FunctionalAreas", Domain::getFunctionalAreas);

  /**
   * The attribute name as used in the data model.
   */
  private final String attributeName;

  /**
   * Function to extract value from Domain.
   */
  private final Function<Domain, Object> getter;

  /**
   * Lookup map for case-insensitive attribute resolution.
   */
  private static final Map<String, DomainAttribute> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              attr -> attr.attributeName.toLowerCase(Locale.ROOT),
              attr -> attr));

  DomainAttribute(
      final String attributeName,
      final Function<Domain, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the value of the mapped attribute from the specified domain.
   *
   * @param domain the domain from which the attribute value is extracted
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  @Override
  public Object extractValue(final Domain domain) {
    return getter.apply(domain);
  }

  /**
   * Returns the {@code DomainAttribute} enum constant associated with the specified
   * attribute name.
   *
   * <p>Attribute name matching is performed in a case-insensitive manner.</p>
   *
   * @param fieldName the attribute name to resolve
   * @return the matching {@code DomainAttribute} enum constant
   * @throws WorkdayException if no {@code DomainAttribute} constant exists for the
   *                          specified attribute name
   */
  public static DomainAttribute fromName(final String fieldName)
      throws WorkdayException {
    DomainAttribute attribute = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (attribute == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return attribute;
  }
}
