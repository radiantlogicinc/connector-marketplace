package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a job location returned from the Workday API.
 *
 * <p>This class contains location-related details such as identifiers,
 * descriptive information, and associated country.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobLocation {

  /**
   * Unique identifier of the location.
   */
  @SerializedName("id")
  private String locId;

  /**
   * Human-readable description of the location.
   */
  private String descriptor;

  /**
   * External location identifier.
   */
  @SerializedName("Location_ID")
  private String locationId;

  /**
   * Country associated with the location.
   */
  private Country country;
}