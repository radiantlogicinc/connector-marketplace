package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a worker record received from the source system.
 * Contains worker details, personal information, job information,
 * and worker classification data.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
    justification = "DTO used for API serialization/deserialization")
public class WorkerRow {
  /**
   * Unique identifier of the worker in the source system.
   */
  @SerializedName("id")
  private String workerId;

  /**
   * Display description of the worker.
   */
  private String descriptor;

  /**
   * Personal information associated with the worker.
   */
  private Person person;

  /**
   * Details of the worker's primary job assignment.
   */
  private PrimaryJob primaryJob;

  /**
   * Classification of the worker.
   */
  private WorkerType workerType;

  /**
   * Business identifier assigned to the worker.
   */
  @SerializedName("workerId")
  private String workerSpecificId;

}