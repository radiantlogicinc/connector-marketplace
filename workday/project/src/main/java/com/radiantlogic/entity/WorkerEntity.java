package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import com.radiantlogic.model.Person;
import com.radiantlogic.model.PrimaryJob;
import com.radiantlogic.model.WorkerType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a worker within the organization.
 *
 * <p>This entity contains identifying information, resource references,
 * organizational hierarchy details, and management status associated
 * with a worker.</p>
 */
@Entity(name = "worker", namingKey = "worker")
@Getter
@NoArgsConstructor
public class WorkerEntity {

  /**
   * Unique identifier of the worker.
   * This attribute serves as the naming attribute for the entity.
   */
  @Attribute(isNamingAttribute = true)
  private String workerId;

  /**
   * Human-readable description or display name associated with the worker.
   */
  @Attribute(isNullable = true)
  private String descriptor;

  /**
   * Person information associated with the worker.
   */
  @Attribute(isNullable = true)
  private Person person;

  /**
   * Primary job information for the worker.
   */
  @Attribute(isNullable = true)
  private PrimaryJob primaryJob;
  /**
   * Worker type information.
   */
  @Attribute(isNullable = true)
  private WorkerType workerType;

  /**
   * Worker identifier.
   */
  @Attribute(isNullable = true)
  private String workerSpecificId;

}
