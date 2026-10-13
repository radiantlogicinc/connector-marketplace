package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Generic response object for Workday reports.
 *
 * <p>This class represents the JSON response structure for Workday reports,
 * containing a list of report entries of any type.</p>
 *
 * @param <T> the type of report entries
 */
@Getter
@NoArgsConstructor
public class ReportResponse<T> {

  /**
   * List of report entries from the report.
   */
  @Getter(onMethod_ = @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "DTO intentionally exposes mutable collection"))
  @SerializedName("Report_Entry")
  private List<T> reportEntry;
}
