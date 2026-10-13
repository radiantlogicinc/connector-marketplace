package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO representing a narrow projection of records from the sys_user_has_role API. Only exposes
 * selected fields: role.name and user.user_name from the underlying table.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserRoleRow {

  /**
   * Stores role name.
   */
  @SerializedName("role.name")
  private String roleName;
  /**
   * Stores user name.
   */
  @SerializedName("user.user_name")
  private String userName;
}
