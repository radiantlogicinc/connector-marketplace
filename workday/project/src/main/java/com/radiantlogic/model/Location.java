package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Represents a worker location returned in the response payload.
 *
 * <p>Contains location related details associated with a worker.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.ShortVariable")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
    justification = "DTO used for API serialization/deserialization")
public class Location {

  /**
   * Human-readable description of the location.
   */
  private String descriptor;
  /**
   * Unique identifier of the location in the source system.
   */
  private String id;
  /**
   * External location identifier.
   */
  @SerializedName("Location_ID")
  private String locationId;
  /**
   * Country information.
   */
  private Country country;
}
