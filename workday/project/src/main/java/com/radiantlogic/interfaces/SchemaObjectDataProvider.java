package com.radiantlogic.interfaces;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.utility.SearchResultEntry;
import java.io.IOException;
import java.util.List;

/**
 * Interface for handling paginated retrieval of schema object data.
 *
 * <p>Implementations are responsible for fetching data for specific schema objects
 * (for example, workers) and maintaining pagination state across requests.</p>
 */
public interface SchemaObjectDataProvider {

  /**
   * Retrieves the next page of results for a schema object.
   *
   * @param pageSize set the page size to limit number for records per page. This pageSize must be
   *                 same as the one defined in Request Builder.
   * @return a list of LDAP search result entries representing the next page
   * @throws IOException      if an I/O error occurs during data retrieval
   * @throws WorkdayException if the request fails or the response is invalid
   */
  List<SearchResultEntry> getNextPage(int pageSize)
      throws IOException, WorkdayException;

  /**
   * Indicates whether additional records are available for retrieval.
   *
   * @return {@code true} if more pages exist, {@code false} otherwise
   */
  boolean hasMoreRecords();
}
