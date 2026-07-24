package com.radiantlogic.schema;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.NoArgsConstructor;

import static com.radiantlogic.utility.Constants.PMD_KEY;

/**
 * Defines the {@code account} schema object for IDDM automatic schema authoring.
 *
 * <p>This class exists only to define the {@code account} schema object and its attributes; its
 * fields are never populated or read at runtime. The {@code account} object is read-only and uses
 * {@code ID} as its naming attribute. All attributes are exposed as strings because the connector
 * returns every value to IDDM as a string.
 */
@Entity(name = "account")
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public final class AccountEntity {

  /** Naming attribute (primary key and RDN) of the account schema object. */
  @Attribute(name = "ID", isNamingAttribute = true)
  private String id;

  /** Name of the account. */
  @Attribute(name = "Name", isNullable = true)
  private String name;

  /** Target system address. */
  @Attribute(name = "Address", isNullable = true)
  private String address;

  /** User name associated with the account. */
  @Attribute(name = "UserName", isNullable = true)
  private String userName;

  /** Platform identifier. */
  @Attribute(name = "PlatformId", isNullable = true)
  private String platformId;

  /** Name of the owning safe. */
  @Attribute(name = "SafeName", isNullable = true)
  private String safeName;

  /** Type of the managed secret. */
  @Attribute(name = "SecretType", isNullable = true)
  private String secretType;

  /** Creation timestamp of the account. */
  @Attribute(name = "CreatedTime", isNullable = true)
  private String createdTime;

  /** Secret management settings. */
  @Attribute(name = "SecretManagement", isNullable = true)
  private String secretManagement;

  /** Platform-specific properties. */
  @Attribute(name = "PlatformAccountProperties", isNullable = true)
  private String platformAccountProperties;

  /** Category modification timestamp. */
  @Attribute(name = "CategoryModificationTime", isNullable = true)
  private String categoryModificationTime;

  /** Remote access configuration. */
  @Attribute(name = "RemoteMachinesAccess", isNullable = true)
  private String remoteMachinesAccess;
}
