package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a geographic region associated with a job posting location.
 *
 * <p>This model contains region information returned by the Workday API,
 * including the region name and region code.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Region {

  /**
   * Human-readable name or description of the region.
   */
  private String descriptor;

  /**
   * Region code associated with the location.
   */
  private String code;
}