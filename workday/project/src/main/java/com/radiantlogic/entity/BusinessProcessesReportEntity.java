package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a business process from Workday report.
 *
 * <p>This entity contains information about business processes including
 * their ID, name, description, and functional areas.</p>
 */
@Entity(name = "businessprocess", namingKey = "businessprocess")
@Getter
@ToString
@NoArgsConstructor
public class BusinessProcessesReportEntity {

  /**
   * Unique identifier of the business process.
   */
  @Attribute()
  private String workdayId;

  /**
   * Name of the business process. This attribute serves as the naming attribute for the entity.
   */
  @Attribute(isNamingAttribute = true)
  private String name;

  /**
   * Description of the business process.
   */
  @Attribute(isNullable = true)
  private String description;

  /**
   * Functional areas associated with the business process.
   */
  @Attribute(isNullable = true)
  private String functionalAreas;
}
