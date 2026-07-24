package com.radiantlogic.schema;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.NoArgsConstructor;

import static com.radiantlogic.utility.Constants.PMD_KEY;

/**
 * Defines the {@code role} schema object for IDDM automatic schema authoring.
 *
 * <p>This class exists only to define the {@code role} schema object and its attributes; its
 * fields are never populated or read at runtime. All attributes are exposed as strings because the
 * connector returns every value to IDDM as a string.
 */
@Entity(name = "role")
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public final class RoleEntity {

  /** Naming attribute (primary key and RDN) of the role schema object. */
  @Attribute(name = "Name", isNamingAttribute = true)
  private String name;

  /** Backend-generated role identifier. */
  @Attribute(name = "ID", isNullable = true)
  private String id;

  /** Type of role as defined by CyberArk. */
  @Attribute(name = "RoleType", isNullable = true)
  private String roleType;

  /** Description of the role. */
  @Attribute(name = "Description", isNullable = true)
  private String description;

  /** Multi-valued list of the role's members. */
  @Attribute(name = "Members", isNullable = true)
  private String members;

  /** Multi-valued list of the role's administrative rights. */
  @Attribute(name = "RoleRights", isNullable = true)
  private String roleRights;
}
