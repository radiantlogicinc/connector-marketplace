package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the job site associated with a job posting.
 *
 * <p>This model contains job site details returned by the Workday API,
 * including the job site identifier and display name. A job site may
 * represent a physical site or a designated work location grouping.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobSite {

  /**
   * Unique identifier of the job site.
   */
  @SerializedName("id")
  private String jobSiteId;

  /**
   * Human-readable name or description of the job site.
   */
  private String descriptor;
}