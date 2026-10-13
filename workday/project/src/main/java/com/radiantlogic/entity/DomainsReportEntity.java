package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a domain from Workday report.
 *
 * <p>This entity contains information about domains including
 * their ID, name, description, and functional areas.</p>
 */
@Entity(name = "domain", namingKey = "domain")
@Getter
@ToString
@NoArgsConstructor
public class DomainsReportEntity {

  /**
   * Unique Workday identifier for the security domain.
   */
  @Attribute()
  private String workdayId;
  /**
   * Display name of the Workday security domain.
   */
  @Attribute(isNamingAttribute = true)
  private String name;

  /**
   * Description of the Workday security domain.
   *
   * <p>Provides additional details about the purpose or usage of the domain.
   */
  @Attribute(isNullable = true)
  private String description;

  /**
   * Functional areas associated with the Workday security domain.
   *
   * <p>A domain can be associated with one or more functional areas within
   * Workday.
   */
  @Attribute(isNullable = true)
  private String functionalAreas;
}
