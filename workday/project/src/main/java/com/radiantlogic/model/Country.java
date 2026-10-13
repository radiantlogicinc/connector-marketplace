package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents country information associated with a location.
 * Contains country name and standard country code.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Country {

  /**
   * Display name of the country.
   */
  private String descriptor;

  /**
   * ISO 3166-1 alpha-3 country code.
   */
  @SerializedName("ISO_3166-1_Alpha-3_Code")
  private String isoAlphaCode;



}
