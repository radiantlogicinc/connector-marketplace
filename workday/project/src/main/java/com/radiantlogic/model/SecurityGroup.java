package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a Workday security group.
 *
 * <p>A security group in Workday defines a collection of users or roles that
 * share a common set of security permissions. This model contains identifying
 * information and status attributes for a security group returned by Workday.
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_FIELD",
    justification = "Fields are populated via Gson JSON deserialization using reflection"
)
public class SecurityGroup {

  /**
   * Unique Workday identifier for the security group.
   *
   * <p>This value corresponds to the Workday ID assigned to the security group.
   */
  @SerializedName("id")
  private String workdayId;

  /**
   * Display name of the Workday security group.
   */
  private String name;

  /**
   * Classification of the Workday security group.
   *
   * <p>Examples include role-based, user-based, or other Workday-defined
   * security group types.
   */
  private String type;

  /**
   * Scope associated with the Workday security group.
   *
   * <p>The scope defines the organizational or functional context in which the
   * security group applies. This value may be absent if no scope is defined.
   */
  private String scope;

  /**
   * Indicates whether the Workday security group is inactive.
   *
   * <p>Workday represents this value as a string in the source data.
   */
  @SuppressWarnings("PMD.LinguisticNaming")
  private String isInactive;
}
