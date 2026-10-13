package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for add user to group request payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddUserToGroupRequest {

  /**
   * User name.
   */
  @SerializedName("user")
  private String userName;

  /**
   * Group name.
   */
  @SerializedName("group")
  private String groupName;
}
