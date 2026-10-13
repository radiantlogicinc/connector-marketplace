package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the primary work location associated with a job posting.
 *
 * <p>This model contains location details returned by the Workday API,
 * including the location identifier, display name, country, and region
 * information.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PrimaryLocation {

  /**
   * Unique identifier of the primary location.
   */
  @SerializedName("id")
  private String primaryLocationId;

  /**
   * Human-readable name or description of the primary location.
   */
  private String descriptor;

  /**
   * Country associated with the primary location.
   */
  private JobPostingCountry country;

  /**
   * Region associated with the primary location.
   */
  private Region region;

}