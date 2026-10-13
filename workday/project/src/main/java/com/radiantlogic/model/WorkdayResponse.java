package com.radiantlogic.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper for Workday API responses supporting pagination.
 *
 * <p>Contains response data along with pagination metadata such as
 * total record count.</p>
 */
@Getter
@NoArgsConstructor
public class WorkdayResponse<T> {

  /**
   * List of records returned in the current response.
   */
  @Getter(onMethod_ = @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "DTO intentionally exposes mutable collection"))
  private List<T> data;

  /**
   * Total number of records available for the query.
   */
  private int total;


}
