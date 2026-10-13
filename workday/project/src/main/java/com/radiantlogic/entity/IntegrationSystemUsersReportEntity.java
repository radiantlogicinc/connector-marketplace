package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents an integration system user configured in Workday.
 *
 * <p>An integration system user is a non-interactive account used by external
 * applications, integrations, or middleware to authenticate with Workday and
 * perform authorized operations. This class stores the identifying and
 * administrative attributes associated with the integration system user.
 */
@Entity(name = "integrationsystemuser",
    namingKey = "integrationsystemuser")
@Getter
@NoArgsConstructor
public class IntegrationSystemUsersReportEntity {

  /**
   * Unique Workday identifier assigned to the integration system user.
   */
  @Attribute()
  private String workdayId;

  /**
   * User-defined description that identifies the purpose or usage of the
   * integration system user in Workday.
   */
  @Attribute(isNullable = true)
  private String description;

  /**
   * Indicates whether the integration system user is disabled in Workday.
   *
   * <p>A value of {@code true} indicates that the user is disabled, while
   * {@code false} indicates that the user is enabled.
   */
  @Attribute(isNullable = true)
  @SuppressWarnings("PMD.LinguisticNaming")
  private String isDisabled;

  /**
   * Unique user name used by the integration system user to authenticate with
   * Workday.
   */
  @Attribute(isNamingAttribute = true)
  private String userName;
}
