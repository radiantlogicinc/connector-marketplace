package com.radiantlogic.get;

import com.google.common.collect.ImmutableList;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.JobRow;
import com.radiantlogic.model.WorkdayResponse;
import com.radiantlogic.utility.JobUtil;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import java.io.IOException;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Data provider for retrieving job schema objects from Workday.
 *
 * <p>This class is responsible for fetching job data and transforming it into
 * schema-compliant objects using the configured connector and schema mapping.</p>
 */
@Slf4j
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class JobsDataProvider implements SchemaObjectDataProvider {

  /**
   * Request context used for retrieving job data.
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
   * Retrieves the next page of search results.
   *
   * @param pageSize number of records per page assumed by the caller; used to advance the internal
   *                 pagination offset. The API request page size is fixed in {@code RequestBuilder}
   *                 and is not affected by this parameter.
   * @return a list of LDAP search result entries for the next page
   * @throws IOException      if an I/O error occurs during retrieval
   * @throws WorkdayException if the request fails or returns an invalid response
   */
  @Override
  public List<SearchResultEntry> getNextPage(final int pageSize)
      throws IOException, WorkdayException {
    log.debug("Fetching jobs page: uniqueKey={}, offset={}", uniqueKey, offset);
    final WorkdayResponse<JobRow> workdayResponse =
        getRequest.getJobs(uniqueKey, offset);
    offset += pageSize;
    this.hasMoreRecordsFlag = (offset < workdayResponse.getTotal());
    final List<JobRow> jobs = workdayResponse.getData();
    if (jobs == null) {
      throw new WorkdayException("Workday response data is null for jobs");
    }
    final ImmutableList.Builder<SearchResultEntry> pageEntries = ImmutableList.builder();
    log.debug("Jobs fetched {}", jobs.size());
    if (!jobs.isEmpty()) {
      pageEntries.addAll(
          converter.convert(JobUtil.extractJobsData(jobs, schema, targetSchemaObjects)));
    }
    return pageEntries.build();
  }
}
