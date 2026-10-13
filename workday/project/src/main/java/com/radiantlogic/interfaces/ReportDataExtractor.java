package com.radiantlogic.interfaces;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import java.util.List;
import java.util.Map;

/**
 * Interface for extracting and transforming report data.
 *
 * <p>This interface defines the contract for extracting records from report responses,
 * filtering them by unique keys, and transforming them into a format suitable for
 * LDAP search responses.</p>
 *
 * @param <T> the type of individual records in the report
 * @param <R> the type of the report response object
 */
public interface ReportDataExtractor<T, R> {

  /**
   * Extracts the list of records from the report response.
   *
   * @param response the report response object
   * @return the list of records extracted from the response
   */
  List<T> extractRecords(R response);

  /**
   * Filters the list of records by a unique key.
   *
   * @param records the list of records to filter
   * @param uniqueKey the unique key to filter by
   * @return the filtered list of records
   */
  List<T> filterByUniqueKey(List<T> records, String uniqueKey);

  /**
   * Extracts data from records into a format suitable for conversion.
   *
   * @param records the list of records to extract data from
   * @param schema the schema definition
   * @param targetSchemaObjects the target schema object properties
   * @return a list of maps containing the extracted data
   * @throws WorkdayException if an error occurs during data extraction
   */
  List<Map<String, Object>> extractData(List<T> records, Schema schema,
      ReadOnlyProperties targetSchemaObjects) throws WorkdayException;
}
