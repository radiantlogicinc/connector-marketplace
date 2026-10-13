package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import com.radiantlogic.model.JobLocation;
import com.radiantlogic.model.JobType;
import com.radiantlogic.model.JobWorker;
import com.radiantlogic.model.SupervisoryOrganization;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a Job entity in the Workday system.
 *
 * <p>A job contains identifying information and related attributes such as
 * worker, supervisory organization, location, and job type.</p>
 */
@Entity(name = "job", namingKey = "job")
@Getter
@NoArgsConstructor
public final class JobEntity {

  /**
   * Unique identifier of the job.
   * Used as the naming attribute for the entity.
   */
  @Attribute(isNamingAttribute = true)
  private String jobId;

  /**
   * Human-readable description of the job.
   */
  @Attribute(isNullable = true)
  private String descriptor;

  /**
   * Worker associated with the job.
   */
  @Attribute(isNullable = true)
  private JobWorker jobWorker;

  /**
   * Supervisory organization associated with the job.
   */
  @Attribute(isNullable = true)
  private SupervisoryOrganization supervisoryOrganization;

  /**
   * Location of the job.
   */
  @Attribute(isNullable = true)
  private JobLocation location;

  /**
   * Type classification of the job.
   */
  @Attribute(isNullable = true)
  private JobType jobType;
}