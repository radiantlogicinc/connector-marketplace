package com.radiantlogic.schema;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.NoArgsConstructor;

import static com.radiantlogic.utility.Constants.PMD_KEY;

/**
 * Defines the {@code user} schema object for IDDM automatic schema authoring.
 *
 * <p>This class exists only to define the {@code user} schema object and its attributes; its
 * fields are never populated or read at runtime. All attributes are exposed as strings because the
 * connector returns every value to IDDM as a string.
 */
@Entity(name = "user")
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public final class UserEntity {

  /** Naming attribute (primary key and RDN) of the user schema object. */
  @Attribute(name = "Name", isNamingAttribute = true)
  private String name;

  /** Display name of the user. */
  @Attribute(name = "DisplayName", isNullable = true)
  private String displayName;

  /** Backend-generated user identifier. */
  @Attribute(name = "ID", isNullable = true)
  private String id;

  /** User's risk level rank. */
  @Attribute(name = "RiskLevelRank", isNullable = true)
  private String riskLevelRank;

  /** Status of the user. */
  @Attribute(name = "Status", isNullable = true)
  private String status;

  /** Mobile number of the user. */
  @Attribute(name = "MobileNumber", isNullable = true)
  private String mobileNumber;

  /** Email address of the user. */
  @Attribute(name = "Mail", isNullable = true)
  private String mail;

  /** Directory or source the user originates from. */
  @Attribute(name = "Source", isNullable = true)
  private String source;
}
