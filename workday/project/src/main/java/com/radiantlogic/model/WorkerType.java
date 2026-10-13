package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the classification of a worker in the source system.
 * Contains information used to identify the worker type.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerType {

  /**
   * Display description of the worker type.
   */
  private String descriptor;

  /**
   * Unique identifier of the worker type.
   */
  @SerializedName("id")
  private String workerTypeId;
}
