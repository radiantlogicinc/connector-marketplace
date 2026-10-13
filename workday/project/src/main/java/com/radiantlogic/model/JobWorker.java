package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a worker associated with a job.
 *
 * <p>This class maps worker-related information returned from the Workday API,
 * including the worker identifier and descriptive information.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobWorker {

  /**
   * Unique identifier of the worker.
   */
  @SerializedName("id")
  private String workerId;
  /**
   * Human-readable description of the worker.
   */
  private String descriptor;

}
