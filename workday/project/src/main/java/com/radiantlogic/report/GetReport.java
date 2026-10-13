package com.radiantlogic.report;

import com.google.common.collect.ImmutableList;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.ReportDataExtractor;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Generic data provider responsible for retrieving report data.
 *
 * <p>Implements schema object data retrieval logic for report entities,
 * supporting fetching data from configured report URLs. This class is generic and can handle
 * different types of reports through the ReportDataExtractor interface.</p>
 *
 * @param <T> the type of individual records in the report
 * @param <R> the type of the report response object
 */
@Slf4j
@Builder
public class GetReport<T, R> implements SchemaObjectDataProvider {

  /**
   * Request context used for retrieving report data.
   */
  private final GetRequest getRequest;

  /**
   * Converter used to transform response data into internal models.
   */
  private final SearchResponseConverter converter;

  /**
   * Schema definition associated with the requested data.
   */
  private final Schema schema;

  /**
   * Schema configuration describing the requested objects and attributes.
   */
  private final ReadOnlyProperties targetSchemaObjects;

  /**
   * URL from which to fetch the report data.
   */
  private final String reportUrl;

  /**
   * Unique identifier to filter a specific record from the report. If null or empty, all records
   * are returned.
   */
  private final String uniqueKey;

  /**
   * Type of the report response object (supports generics).
   */
  private final Type responseType;

  /**
   * Data extractor for transforming report data.
   */
  private final ReportDataExtractor<T, R> dataExtractor;

  /**
   * Indicates whether additional records are available for pagination.
   */
  @Builder.Default
  private boolean hasMoreRecordsFlag = true;

  @Override
  public boolean hasMoreRecords() {
    return hasMoreRecordsFlag;
  }

  @Override
  public List<SearchResultEntry> getNextPage(final int pageSize)
      throws IOException, WorkdayException {
    if (!hasMoreRecordsFlag) {
      return new LinkedList<>();
    }

    final R reportResponse = getRequest.getReport(reportUrl, responseType);

    List<T> records = dataExtractor.extractRecords(reportResponse);
    if (records == null) {
      throw new WorkdayException("Report response data is null");
    }

    log.debug("Records fetched: {}", records.size());

    if (StringUtils.isNotBlank(uniqueKey)) {
      log.debug("Filtering report by uniqueKey: {}", uniqueKey);
      records = dataExtractor.filterByUniqueKey(records, uniqueKey);
      log.debug("Filtered records count: {}", records.size());
    }

    this.hasMoreRecordsFlag = false;

    final ImmutableList.Builder<SearchResultEntry> pageEntries = ImmutableList.builder();
    if (!records.isEmpty()) {
      pageEntries.addAll(
          converter.convert(
              dataExtractor.extractData(records, schema, targetSchemaObjects)));
    }
    return pageEntries.build();
  }
}
