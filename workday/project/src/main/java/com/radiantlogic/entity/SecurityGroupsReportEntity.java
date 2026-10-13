package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Workday security group.
 *
 * <p>A security group in Workday defines a collection of users or roles that
 * share a common set of security permissions. This model contains identifying
 * information and status attributes for a security group returned by Workday.
 */
@Entity(name = "securitygroup", namingKey = "securitygroup")
@Getter
@ToString
@NoArgsConstructor
public class SecurityGroupsReportEntity {

  /**
   * Unique Workday identifier for the security group.
   *
   * <p>This value corresponds to the Workday ID assigned to the security group.
   */
  @Attribute(isNamingAttribute = true)
  private String workdayId;

  /**
   * Display name of the Workday security group.
   */
  @Attribute(isNullable = true)
  private String name;

  /**
   * Classification of the Workday security group.
   *
   * <p>Examples include role-based, user-based, or other Workday-defined
   * security group types.
   */
  @Attribute(isNullable = true)
  private String type;

  /**
   * Scope associated with the Workday security group.
   *
   * <p>The scope defines the organizational or functional context in which the
   * security group applies. This value may be absent if no scope is defined.
   */
  @Attribute(isNullable = true)
  private String scope;

  /**
   * Indicates whether the Workday security group is inactive.
   *
   * <p>Workday represents this value as a string in the source data.
   */
  @Attribute(isNullable = true)
  @SuppressWarnings("PMD.LinguisticNaming")
  private String isInactive;
}
