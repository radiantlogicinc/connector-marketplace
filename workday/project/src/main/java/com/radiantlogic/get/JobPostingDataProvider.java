package com.radiantlogic.get;

import com.google.common.collect.ImmutableList;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.JobPostingRow;
import com.radiantlogic.model.WorkdayResponse;
import com.radiantlogic.utility.JobPostingUtil;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import java.io.IOException;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Data provider for retrieving job posting schema objects from Workday.
 *
 * <p>This class is responsible for fetching job posting data and transforming it into
 * schema-compliant objects using the configured connector and schema mapping.</p>
 */
@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class JobPostingDataProvider implements SchemaObjectDataProvider {

  /**
   * Request context used for retrieving job posting data.
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
   * Unique identifier used when fetching a single job record.
   */
  private final String uniqueKey;

  /**
   * Indicates whether additional records are available for pagination.
   */
  @Builder.Default
  private boolean hasMoreRecordsFlag = true;

  /**
   * Current pagination offset used for retrieving the next set of results.
   */
  private int offset;

  /**
   * Indicates whether additional records are available for retrieval.
   *
   * @return {@code true} if more records are available, {@code false} otherwise
   */
  @Override
  public boolean hasMoreRecords() {
    return hasMoreRecordsFlag;
  }

  /**
   * Retrieves the next page of job posting search results.
   *
   * <p>This method fetches the next set of records based on the current
   * pagination state and returns the results as search result entries.</p>
   *
   * @param pageSize maximum number of records to retrieve in a single page. This pageSize must be *
   *                 same as the one defined in Request Builder.
   * @return a list of {@link SearchResultEntry} objects representing the next page of job posting
   *          results
   * @throws IOException      if an error occurs while communicating with the Workday service
   * @throws WorkdayException if the Workday API returns an error or the response cannot be
   *                          processed
   */
  @Override
  public List<SearchResultEntry> getNextPage(final int pageSize)
      throws IOException, WorkdayException {
    log.debug("Fetching job postings page: uniqueKey={}, offset={}", uniqueKey, offset);
    final WorkdayResponse<JobPostingRow> workdayResponse =
        getRequest.getJobPostings(uniqueKey, offset);
    offset += pageSize;
    this.hasMoreRecordsFlag = (offset < workdayResponse.getTotal());
    final List<JobPostingRow> jobPostings = workdayResponse.getData();
    if (jobPostings == null) {
      throw new WorkdayException("Workday response data is null for jobs");
    }
    final ImmutableList.Builder<SearchResultEntry> pageEntries = ImmutableList.builder();
    log.debug("Job postings fetched {}", jobPostings.size());
    if (!jobPostings.isEmpty()) {
      pageEntries.addAll(
          converter.convert(
              JobPostingUtil.extractJobPostingsData(jobPostings, schema, targetSchemaObjects)));
    }
    return pageEntries.build();
  }

}
