package com.radiantlogic.interfaces;

/**
 * Represents an attribute that can be extracted from a report record.
 *
 * <p>Implementations provide the external attribute name used in report
 * schemas and the logic required to extract the corresponding value from
 * a record.</p>
 *
 * @param <T> the type of report record
 */
public interface ReportAttribute<T> {

  /**
   * Returns the external name of this attribute.
   *
   * @return the attribute name
   */
  String getAttributeName();

  /**
   * Extracts the value of this attribute from the specified record.
   *
   * @param record the report record from which to extract the value
   * @return the extracted attribute value, or {@code null} if no value is available
   */
  Object extractValue(T record);
}
