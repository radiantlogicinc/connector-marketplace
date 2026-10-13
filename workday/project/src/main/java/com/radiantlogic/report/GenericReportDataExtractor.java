package com.radiantlogic.report;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.ReportAttribute;
import com.radiantlogic.interfaces.ReportDataExtractor;
import com.radiantlogic.model.ReportResponse;
import com.radiantlogic.utility.ReportDataExtractorUtil;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic data extractor for report responses.
 *
 * <p>This class provides a reusable implementation for extracting and transforming
 * report data using attribute enums. It eliminates the need for separate extractor classes for each
 * report type.</p>
 *
 * @param <T> the type of individual records in the report
 */
@Slf4j
public final class GenericReportDataExtractor<T>
    implements ReportDataExtractor<T, ReportResponse<T>> {

  /**
   * Function to resolve attribute enum from field name.
   */
  private final ReportDataExtractorUtil.CheckedFunction<String, ReportAttribute<T>>
      attributeResolver;

  /**
   * Function to extract unique key from model object.
   */
  private final Function<T, String> uniqueKeyExtractor;

  /**
   * Creates a new generic report data extractor.
   *
   * @param attributeResolver  function to resolve attribute enum from field name
   * @param uniqueKeyExtractor function to extract unique key from model object
   */
  private GenericReportDataExtractor(
      final ReportDataExtractorUtil.CheckedFunction<String, ReportAttribute<T>> attributeResolver,
      final Function<T, String> uniqueKeyExtractor) {
    this.attributeResolver = attributeResolver;
    this.uniqueKeyExtractor = uniqueKeyExtractor;
  }

  /**
   * Creates a {@link GenericReportDataExtractor} with the provided attribute resolver and unique
   * key extractor.
   *
   * @param <T>                the type of the report data
   * @param attributeResolver  resolves a report attribute from its string representation
   * @param uniqueKeyExtractor extracts the unique key from a report data object
   * @return a new {@link GenericReportDataExtractor} instance
   */
  @SuppressWarnings("PMD.ShortMethodName")
  public static <T> GenericReportDataExtractor<T> of(
      final ReportDataExtractorUtil.CheckedFunction<String, ReportAttribute<T>> attributeResolver,
      final Function<T, String> uniqueKeyExtractor) {

    return new GenericReportDataExtractor<>(
        attributeResolver, uniqueKeyExtractor);

  }

  @Override
  public List<T> extractRecords(final ReportResponse<T> response) {
    return response.getReportEntry();
  }

  @Override
  public List<T> filterByUniqueKey(final List<T> records, final String uniqueKey) {
    return records.stream()
        .filter(record ->
            uniqueKey.equalsIgnoreCase(
                uniqueKeyExtractor.apply(record)))
        .collect(Collectors.toList());
  }

  @Override
  public List<Map<String, Object>> extractData(final List<T> records, final Schema schema,
                                               final ReadOnlyProperties targetSchemaObjects)
      throws WorkdayException {
    return ReportDataExtractorUtil.extractData(
        records,
        schema,
        targetSchemaObjects,
        attributeResolver);
  }
}