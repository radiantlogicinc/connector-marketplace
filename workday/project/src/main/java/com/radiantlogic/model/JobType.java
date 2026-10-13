package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the type of worker's job assignment.
 * Contains information used to classify the job.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobType {

  /**
   * Display description of the job type.
   */
  private String descriptor;

  /**
   * Identifier of the job type.
   */
  @SerializedName("Job_Type_Name_ID")
  private String jobTypeNameId;


}
