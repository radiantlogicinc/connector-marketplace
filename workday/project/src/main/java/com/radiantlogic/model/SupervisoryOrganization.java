package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the supervisory organization associated with a worker's job.
 * Contains identifying information for the organization.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupervisoryOrganization {

  /**
   * Unique identifier of the supervisory organization.
   */
  @SerializedName("id")
  private String supervisoryOrganizationId;
  /**
   * Display description of the supervisory organization.
   */
  private String descriptor;

  /**
   * Reference identifier of the supervisory organization.
   */
  @SerializedName("Organization_Reference_ID")
  private String organizationReferenceId;

}
