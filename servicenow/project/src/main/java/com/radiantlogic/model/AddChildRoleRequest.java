package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for add child role to parent role request payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddChildRoleRequest {

  /**
   * Parent role name.
   */
  @SerializedName("role")
  private String parentRoleName;

  /**
   * Child role name.
   */
  @SerializedName("contains")
  private String childRoleName;
}
