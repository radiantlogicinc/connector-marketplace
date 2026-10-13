package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a security group membership from Workday report.
 *
 * <p>This entity contains information about security group memberships including
 * the group name, security group ID, and members.</p>
 */
@Entity(name = "securitygroupmembership", namingKey = "securitygroupmembership")
@Getter
@ToString
@NoArgsConstructor
public class SecurityGroupsMembershipsReportEntity {

  /**
   * Display name of the security group membership.
   */
  @Attribute(isNullable = true)
  private String name;

  /**
   * Unique Workday identifier for the security group.
   */
  @Attribute(isNamingAttribute = true)
  private String workdayId;

  /**
   * Members of the security group as a list (optional).
   */
  @Getter(onMethod_ = @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "DTO intentionally exposes mutable collection"))
  @Attribute(isNullable = true)
  private List<String> members;
}
