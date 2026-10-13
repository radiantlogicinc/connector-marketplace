package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the company associated with a job posting.
 *
 * <p>This model contains the identifier and display value for a company
 * returned by the Workday API.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Company {

  /**
   * Unique identifier of the company.
   */
  @SerializedName("id")
  private String companyId;

  /**
   * Human-readable name of the company.
   */
  private String descriptor;
}
