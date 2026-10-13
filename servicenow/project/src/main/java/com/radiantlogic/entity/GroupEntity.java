package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import java.util.List;
import lombok.NoArgsConstructor;

/**
 * Class to define group entity with attributes.
 */
@Entity(name = "group", namingKey = "group")
@NoArgsConstructor
public class GroupEntity {

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
   * Mapped to active attribute.
   */
  @Attribute()
  private String active;
  /**
   * Mapped to source attribute.
   */
  @Attribute(isNullable = true)
  private String source;
  /**
   * Mapped to type attribute.
   */
  @Attribute(isNullable = true)
  private String type;
  /**
   * Mapped to includeMembers attribute.
   */
  @Attribute()
  private String includeMembers;
  /**
   * Mapped to sysCreatedBy attribute.
   */
  @Attribute(isNullable = true)
  private String sysCreatedBy;
  /**
   * Mapped to groupUsers attribute.
   */
  @Attribute(isNullable = true)
  private List<String> groupUsers;
  /**
   * Mapped to groupRoles attribute.
   */
  @Attribute(isNullable = true)
  private List<String> groupRoles;
}
