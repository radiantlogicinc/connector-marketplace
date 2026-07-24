package com.radiantlogic.schema;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.NoArgsConstructor;

import static com.radiantlogic.utility.Constants.PMD_KEY;

/**
 * Defines the {@code safe} schema object for IDDM automatic schema authoring.
 *
 * <p>This class exists only to define the {@code safe} schema object and its attributes; its
 * fields are never populated or read at runtime. The {@code safe} object is read-only. All
 * attributes are exposed as strings because the connector returns every value to IDDM as a string.
 */
@Entity(name = "safe")
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public final class SafeEntity {

  /** Naming attribute (primary key and RDN) of the safe schema object. */
  @Attribute(name = "Name", isNamingAttribute = true)
  private String name;

  /** Safe number. */
  @Attribute(name = "Number", isNullable = true)
  private String number;

  /** Description of the safe. */
  @Attribute(name = "Description", isNullable = true)
  private String description;

  /** Multi-valued list of the safe's members. */
  @Attribute(name = "Members", isNullable = true)
  private String members;

  /** Safe location path. */
  @Attribute(name = "Location", isNullable = true)
  private String location;

  /** Central Policy Manager managing the safe. */
  @Attribute(name = "ManagingCpm", isNullable = true)
  private String managingCpm;

  /** Whether the safe has an expired member. */
  @Attribute(name = "IsExpiredMember", isNullable = true)
  private String isExpiredMember;

  /** Creation timestamp of the safe. */
  @Attribute(name = "Created", isNullable = true)
  private String created;

  /** Last modification timestamp of the safe. */
  @Attribute(name = "Modified", isNullable = true)
  private String modified;

  /** Name of the safe's creator. */
  @Attribute(name = "Creator", isNullable = true)
  private String creator;

  /** Whether object-level access control is enabled. */
  @Attribute(name = "IsOlacEnabled", isNullable = true)
  private String isOlacEnabled;

  /** Number of retained versions. */
  @Attribute(name = "NumberOfVersionsRetention", isNullable = true)
  private String numberOfVersionsRetention;

  /** Number of retention days. */
  @Attribute(name = "NumberOfDaysRetention", isNullable = true)
  private String numberOfDaysRetention;

  /** Whether auto purge is enabled. */
  @Attribute(name = "IsAutoPurgeEnabled", isNullable = true)
  private String isAutoPurgeEnabled;
}
