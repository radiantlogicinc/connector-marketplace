package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the time classification associated with a job posting.
 *
 * <p>This model contains the identifier and display value for a time type
 * returned by the Workday API, such as full-time or part-time.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TimeType {

  /**
   * Unique identifier of the time type.
   */
  @SerializedName("id")
  private String timeTypeId;

  /**
   * Human-readable name or description of the time type.
   */
  private String descriptor;

}