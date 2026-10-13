package com.radiantlogic.enums;

import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.interfaces.ReportAttribute;
import com.radiantlogic.model.BusinessProcess;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * Enum representing supported business Process attributes.
 *
 * <p>Each constant maps an external attribute name to a getter that extracts
 * the corresponding value from a {@link BusinessProcess}, enabling attribute-driven data extraction
 * without switch statements.
 */
@Getter
public enum BusinessProcessAttribute implements ReportAttribute<BusinessProcess> {


  WORKDAY_ID("WorkdayId", BusinessProcess::getWorkdayId),
  NAME("Name", BusinessProcess::getName),
  DESCRIPTION("Description", BusinessProcess::getDescription),
  FUNCTIONAL_AREAS("FunctionalAreas", BusinessProcess::getFunctionalAreas);

  /**
   * The attribute name as used in the data model.
   */
  private final String attributeName;

  /**
   * Function to extract value from BusinessProcess.
   */
  private final Function<BusinessProcess, Object> getter;

  /**
   * Lookup map for case-insensitive attribute resolution.
   */
  private static final Map<String, BusinessProcessAttribute> LOOKUP =
      Arrays.stream(values())
          .collect(ImmutableMap.toImmutableMap(
              attr -> attr.attributeName.toLowerCase(Locale.ROOT),
              attr -> attr));

  BusinessProcessAttribute(
      final String attributeName,
      final Function<BusinessProcess, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the value of the mapped attribute from the specified business process.
   *
   * @param businessProcess the business process from which the attribute value is extracted
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  @Override
  public Object extractValue(final BusinessProcess businessProcess) {
    return getter.apply(businessProcess);
  }

  /**
   * Returns the {@code BusinessProcessAttribute} enum constant associated with the specified
   * attribute name.
   *
   * <p>Attribute name matching is performed in a case-insensitive manner.</p>
   *
   * @param fieldName the attribute name to resolve
   * @return the matching {@code BusinessProcessAttribute} enum constant
   * @throws WorkdayException if no {@code BusinessProcessAttribute} constant exists for the
   *                          specified attribute name
   */
  public static BusinessProcessAttribute fromName(final String fieldName)
      throws WorkdayException {
    BusinessProcessAttribute attribute = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (attribute == null) {
      throw new WorkdayException(String.format("Invalid attribute name: %s", fieldName));
    }
    return attribute;
  }
}
