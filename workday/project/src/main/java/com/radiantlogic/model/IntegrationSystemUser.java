package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents an integration system user configured in Workday.
 *
 * <p>An integration system user is a non-interactive account used by external
 * applications, integrations, or middleware to authenticate with Workday and perform authorized
 * operations. This class stores the identifying and administrative attributes associated with the
 * integration system user.
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_FIELD",
    justification = "Fields are populated via Gson JSON deserialization using reflection"
)
public class IntegrationSystemUser {

  /**
   * User-defined description that identifies the purpose or usage of the integration system user in
   * Workday.
   */
  @SerializedName("creationDetails")
  private String description;

  /**
   * Indicates whether the integration system user is disabled in Workday.
   *
   * <p>A value of {@code true} indicates that the user is disabled, while
   * {@code false} indicates that the user is enabled.
   */
  @SuppressWarnings("PMD.LinguisticNaming")
  private String isDisabled;

  /**
   * Unique user name used by the integration system user to authenticate with Workday.
   */
  private String userName;

  /**
   * Unique Workday identifier assigned to the integration system user.
   */
  @SerializedName("id")
  private String workdayId;
}
