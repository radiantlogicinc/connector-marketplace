package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * Represents the primary supervisory organization of a worker.
 *
 * <p>Maps supervisory organization details from the response payload,
 * including identifier, description, and resource reference.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimarySupervisoryOrganization {

  /**
   * Human-readable description of the supervisory organization.
   */
  private String descriptor;

  /**
   * Unique identifier of the supervisory organization in the source system.
   */
  @SerializedName("id")
  private String primarySupervisoryOrganizationId;

  /**
   * Hyperlink reference to the supervisory organization resource.
   */
  private String href;
}
