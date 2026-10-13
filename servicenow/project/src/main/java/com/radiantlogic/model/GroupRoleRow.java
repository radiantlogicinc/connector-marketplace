package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for group role object from sys_group_has_role API.
 */
@Getter
@Setter
@NoArgsConstructor
public class GroupRoleRow {

  /**
   * Role name.
   */
  @SerializedName("role.name")
  private String roleName;

  /**
   * Group name.
   */
  @SerializedName("group.name")
  private String groupName;
}
