package com.radiantlogic.interfaces;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.utility.SearchResultEntry;
import java.io.IOException;
import java.util.List;

/**
 * Interface for handling schema object specific paginated requests. Implementations fetch data for
 * specific schema objects (User etc.) and maintain pagination state.
 */
public interface SchemaObjectDataProvider {

  /**
   * Fetches next page data for a schema object.
   *
   * @return List of search result entries
   */
  List<SearchResultEntry> getNextPage()
      throws IOException, ServiceNowException;

  /**
   * Returns url to get next set of paginated records.
   */
  String getNextLink();

  /**
   * Returns whether more records exist.
   */
  boolean hasMoreRecords();
}
