package com.radiantlogic.utility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.ReportAttribute;
import java.util.List;
import java.util.Map;

/**
 * Generic utility class for extracting report data using attribute enums.
 *
 * <p>This class provides a reusable pattern for converting model objects into
 * attribute maps based on schema definitions and attribute enums.</p>
 */
public final class ReportDataExtractorUtil {

  private ReportDataExtractorUtil() {
  }

  /**
   * Functional interface for functions that can throw WorkdayException.
   *
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   */
  @FunctionalInterface
  public interface CheckedFunction<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param input the function argument
     * @return the function result
     * @throws WorkdayException if an error occurs
     */
    R apply(T input) throws WorkdayException;
  }

  /**
   * Extracts report data into a list of attribute maps based on schema configuration.
   *
   * @param <T>               the type of model objects
   * @param records           list of model records to process
   * @param schema            schema definition used to determine available fields
   * @param targetSchema      schema properties describing the target object configuration
   * @param attributeResolver function to resolve attribute enum from field name
   * @return list of data maps where each map represents a record
   * @throws WorkdayException if an invalid or unsupported field is encountered
   */
  public static <T> List<Map<String, Object>> extractData(
      final List<T> records,
      final Schema schema,
      final ReadOnlyProperties targetSchema,
      final CheckedFunction<String, ReportAttribute<T>> attributeResolver)
      throws WorkdayException {

    final ImmutableList.Builder<Map<String, Object>> searchResultData =
        ImmutableList.builder();
    final List<String> fieldNames =
        Util.getFieldNames(
            Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchema)));

    for (T record : records) {
      Map<String, Object> entry = populateMap(
          record, fieldNames, attributeResolver);
      searchResultData.add(entry);
    }

    return searchResultData.build();
  }

  /**
   * Populates a map with attribute values from a model object.
   *
   * @param <T>               the type of model object
   * @param model             the model object
   * @param fieldNames        list of field names to extract
   * @param attributeResolver function to resolve attribute enum from field name
   * @return map containing attribute values
   * @throws WorkdayException if an invalid field is encountered
   */
  private static <T> Map<String, Object> populateMap(
      final T model,
      final List<String> fieldNames,
      final CheckedFunction<String, ReportAttribute<T>> attributeResolver)
      throws WorkdayException {

    final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    for (String fieldName : fieldNames) {
      ReportAttribute<T> attribute = attributeResolver.apply(fieldName);
      if (attribute == null) {
        throw new WorkdayException("No attribute resolver found for field: " + fieldName);
      }
      Object value = attribute.extractValue(model);

      if (Util.isValidValue(value)) {
        builder.put(attribute.getAttributeName(), value);
      }
    }

    return builder.build();
  }
}
