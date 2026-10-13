package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the primary job assignment of a worker.
 * Contains job, location, organization, and job type information.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
    justification = "DTO used for API serialization/deserialization")
public class PrimaryJob {

  /**
   * Unique identifier of the primary job.
   */
  @SerializedName("id")
  private String primaryJobId;

  /**
   * Display description of the primary job.
   */
  private String descriptor;

  /**
   * Location associated with the primary job.
   */
  private Location location;

  /**
   * Supervisory organization associated with the primary job.
   */
  private SupervisoryOrganization supervisoryOrganization;

  /**
   * Type of the primary job.
   */
  private JobType jobType;

}
