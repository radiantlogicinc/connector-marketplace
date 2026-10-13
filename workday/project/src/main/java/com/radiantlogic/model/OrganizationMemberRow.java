package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Workday supervisory organization member record.
 *
 * <p>This class is used to deserialize data related to members of a Workday supervisory
 * organization,
 * including their job assignment, worker details, location, and organizational relationships.
 * </p>
 *
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public final class OrganizationMemberRow {

  /**
   * Unique identifier of the supervisory organization member in Workday.
   */
  @SerializedName("id")
  private String organizationMemberId;

  /**
   * Descriptor providing a human-readable representation of the member.
   */
  private String descriptor;

  /**
   * Supervisory organization to which the worker is assigned in Workday.
   */
  private SupervisoryOrganization supervisoryOrganization;

  /**
   * Job type assigned to the worker within the supervisory organization.
   */
  private JobType jobType;

  /**
   * Work location associated with the worker in Workday.
   */
  private JobLocation location;

  /**
   * Worker details associated with this supervisory organization member.
   */
  private WorkerType worker;
}