package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import java.util.List;
import lombok.NoArgsConstructor;

/**
 * Class to define role entity with attributes.
 */
@Entity(name = "role", namingKey = "role")
@NoArgsConstructor
public class RoleEntity {

  /**
   * Mapped to sysId attribute.
   */
  @Attribute()
  private String sysId;
  /**
   * Mapped to name attribute.
   */
  @Attribute(isNamingAttribute = true)
  private String name;
  /**
   * Mapped to description attribute.
   */
  @Attribute(isNullable = true)
  private String description;
  /**
   * Mapped to grantable attribute.
   */
  @Attribute(isNullable = true)
  private String grantable;
  /**
   * Mapped to elevatedPrivilege attribute.
   */
  @Attribute(isNullable = true)
  private String elevatedPrivilege;
  /**
   * Mapped to suffix attribute.
   */
  @Attribute(isNullable = true)
  private String suffix;
  /**
   * Mapped to scopedAdmin attribute.
   */
  @Attribute(isNullable = true)
  private String scopedAdmin;
  /**
   * Mapped to canDelegate attribute.
   */
  @Attribute(isNullable = true, name = "canDelegate")
  private String delegateCapability;
  /**
   * Mapped to includesRoles attribute.
   */
  @Attribute(isNullable = true)
  private String includesRoles;
  /**
   * Mapped to federatedId attribute.
   */
  @Attribute(isNullable = true)
  private String federatedId;
  /**
   * Mapped to sysCreatedBy attribute.
   */
  @Attribute(isNullable = true)
  private String sysCreatedBy;
  /**
   * Mapped to sysPolicy attribute.
   */
  @Attribute(isNullable = true)
  private String sysPolicy;
  /**
   * Mapped to childRoles attribute.
   */
  @Attribute(isNullable = true)
  private List<String> childRoles;
}
