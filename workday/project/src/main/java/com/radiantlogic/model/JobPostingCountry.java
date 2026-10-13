package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents country information associated with a job posting location.
 * Contains country name and standard country code.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobPostingCountry {

  /**
   * Display name of the country.
   */
  private String descriptor;

  /**
   * Code alpha3Code for country.
   */
  @SerializedName("alpha3Code")
  private String alphaCode;


}
