package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a job record returned from the Workday API.
 *
 * <p>This class contains job-related attributes such as identifiers,
 * descriptive information, and associated entities like worker,
 * supervisory organization, location, and job type.</p>
 */
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JobRow {

  /**
   * Unique identifier of the job.
   */
  @SerializedName("id")
  private String jobId;

  /**
   * Human-readable description of the job.
   */
  private String descriptor;

  /**
   * Worker associated with the job.
   */
  @SerializedName("worker")
  private JobWorker jobWorker;

  /**
   * Supervisory organization associated with the job.
   */
  private SupervisoryOrganization supervisoryOrganization;

  /**
   * Location associated with the job.
   */
  private JobLocation location;

  /**
   * Type classification of the job.
   */
  private JobType jobType;
}
