package com.radiantlogic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents a security group membership from Workday report.
 *
 * <p>This model contains information about security group memberships including
 * the group name, security group ID, and members.</p>
 */
@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@SuppressFBWarnings(
    value = "UWF_UNWRITTEN_FIELD",
    justification = "Fields are populated via Gson JSON deserialization using reflection"
)
public class SecurityGroupMembership {

  /**
   * Name of the security group.
   */
  private String name;

  /**
   * Unique Workday identifier for the security group.
   */
  @SerializedName("id")
  private String workdayId;

  /**
   * Members of the security group (optional).
   */
  private String members;

  /**
   * Returns the members as a list of strings.
   *
   * <p>If members is not null and non-empty, splits the string by '; ' delimiter.
   * Otherwise, returns an empty list.</p>
   *
   * @return list of member strings, or empty list if members is null or empty
   */
  public List<String> getMembersList() {
    if (members != null && !members.isEmpty()) {
      return ImmutableList.copyOf(members.split("; "));
    }
    return ImmutableList.of();
  }
}
