package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for group user object from sys_user_grmember API.
 */
@Getter
@Setter
@NoArgsConstructor
public class GroupUserRow {

  /**
   * Stores group name.
   */
  @SerializedName("group.name")
  private String groupName;
  /**
   * Stores user name.
   */
  @SerializedName("user.user_name")
  private String userName;
}
