package com.radiantlogic.get;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.WorkdayResponse;
import com.radiantlogic.model.WorkerRow;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import com.radiantlogic.utility.WorkerUtil;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Data provider responsible for retrieving worker objects in a paginated manner.
 *
 * <p>Implements schema object data retrieval logic for worker entities,
 * supporting pagination and incremental data fetching.</p>
 */
@Slf4j
@Builder
public final class WorkersDataProvider implements SchemaObjectDataProvider {

  /**
   * Request context used for retrieving worker data.
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
   * Unique identifier used when fetching a single worker record.
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
   * @param pageSize set the page size to limit number for records per page
   * @return a list of LDAP search result entries for the next page
   * @throws IOException if an I/O error occurs during retrieval
   * @throws WorkdayException if the request fails or returns an invalid response
   */
  @Override
  public List<SearchResultEntry> getNextPage(final int pageSize)
      throws IOException, WorkdayException {
    WorkdayResponse<WorkerRow> workdayResponse =
        getRequest.getWorkers(uniqueKey, offset);
    offset += pageSize;
    this.hasMoreRecordsFlag = (offset < workdayResponse.getTotal());
    List<WorkerRow> workers = workdayResponse.getData();
    if (workers == null) {
      throw new WorkdayException("Workday response data is null for workers");
    }
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    log.debug("Workers fetched {}", workers.size());
    if (!workers.isEmpty()) {
      pageEntries.addAll(
          converter.convert(WorkerUtil.extractWorkersData(workers, schema, targetSchemaObjects)));
    }
    return pageEntries;
  }
}
