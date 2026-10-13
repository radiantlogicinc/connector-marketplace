package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for group object.
 */
@Getter
@Setter
@NoArgsConstructor
public class GroupRow {

  /**
   * Stores group active status.
   */
  private String active;
  /**
   * Stores group description.
   */
  private String description;
  /**
   * Stores group source.
   */
  private String source;
  /**
   * Stores group type. Ideally contains comma-separated sys_ids referencing the sys_user_group_type
   * table. However, the ServiceNow API also accepts arbitrary raw string values, which it will
   * persist without validating that a corresponding record exists in sys_user_group_type.
   */
  private String type;
  /**
   * Stores group system id.
   */
  @SerializedName("sys_id")
  private String sysId;
  /**
   * Stores group name.
   */
  private String name;
  /**
   * Stores group include members flag.
   */
  @SerializedName("include_members")
  private String includeMembers;
  /**
   * Stores group created by.
   */
  @SerializedName("sys_created_by")
  private String sysCreatedBy;
  /**
   * Stores list of users in the group.
   */
  private List<String> groupUsers;
  /**
   * Stores list of roles assigned to the group.
   */
  private List<String> groupRoles;
}
