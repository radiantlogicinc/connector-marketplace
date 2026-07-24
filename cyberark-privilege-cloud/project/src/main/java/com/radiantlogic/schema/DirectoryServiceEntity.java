package com.radiantlogic.schema;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.NoArgsConstructor;

/**
 * Defines the {@code directoryservice} schema object for IDDM automatic schema authoring.
 *
 * <p>This class exists only to define the {@code directoryservice} schema object and its
 * attributes; its fields are never populated or read at runtime. The {@code directoryservice}
 * object is read-only. All attributes are exposed as strings because the connector returns every
 * value to IDDM as a string.
 */
@Entity(name = "directoryservice")
@NoArgsConstructor
public final class DirectoryServiceEntity {

  /** Naming attribute (primary key and RDN) of the directory service schema object. */
  @Attribute(name = "Name", isNamingAttribute = true)
  private String name;

  /** Directory service type. */
  @Attribute(name = "Service", isNullable = true)
  private String service;

  /** Display name of the directory service. */
  @Attribute(name = "DisplayName", isNullable = true)
  private String displayName;

  /** Tenant identifier. */
  @Attribute(name = "Tenant", isNullable = true)
  private String tenant;

  /** Status of the directory service. */
  @Attribute(name = "Status", isNullable = true)
  private String status;

  /** Configuration summary. */
  @Attribute(name = "Config", isNullable = true)
  private String config;

  /** Whether sync rules are configured. */
  @Attribute(name = "SyncRulesConfigured", isNullable = true)
  private String syncRulesConfigured;

  /** Display version of the status. */
  @Attribute(name = "StatusDisplay", isNullable = true)
  private String statusDisplay;

  /** Visibility flag. */
  @Attribute(name = "Everybody", isNullable = true)
  private String everybody;

  /** Description of the directory service. */
  @Attribute(name = "Description", isNullable = true)
  private String description;

  /** Status of the most recent sync job. */
  @Attribute(name = "SyncJobStatus", isNullable = true)
  private String syncJobStatus;

  /** Short display name. */
  @Attribute(name = "DisplayNameShort", isNullable = true)
  private String displayNameShort;

  /** Unique directory service identifier. */
  @Attribute(name = "DirectoryServiceUuid", isNullable = true)
  private String directoryServiceUuid;
}
