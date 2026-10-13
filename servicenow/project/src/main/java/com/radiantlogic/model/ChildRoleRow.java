package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for child role object from sys_user_role_contains API.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChildRoleRow {

  /**
   * Stores parent role name.
   */
  @SerializedName("role.name")
  private String parentRoleName;
  /**
   * Stores child role name.
   */
  @SerializedName("contains.name")
  private String childRoleName;
}
