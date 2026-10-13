package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the job type associated with a job posting.
 *
 * <p>This model contains job type details returned by the Workday API,
 * including the identifier and display name. Job type may represent
 * employment classification such as permanent, contract, or temporary.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobPostingJobType {

  /**
   * Unique identifier of the job type.
   */
  @SerializedName("id")
  private String jobTypeId;

  /**
   * Human-readable name or description of the job type.
   */
  private String descriptor;
}