package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store data used to prepare payload for create role object.
 */
@Getter
@Setter
@NoArgsConstructor
public class RolePojo {

  /**
   * This field will store role name.
   */
  @SerializedName("Name")
  private String name;
  /**
   * This field will store role description.
   */
  @SerializedName("Description")
  private String description;
  /**
   * This field will store role type.
   */
  @SerializedName("RoleType")
  private String roleType;
  /**
   * This field will store role members of type ROLE.
   */
  @SerializedName("Roles")
  private final Set<String> roles = new HashSet<>();
  /**
   * This field will store role members of type USER.
   */
  @SerializedName("Users")
  private final Set<String> users = new HashSet<>();

  /**
   * This method will assign a list to roles member field.
   *
   * @param roles List which has to be assigned
   */
  public void setRoles(final Set<String> roles) {
    this.roles.clear();
    if (roles != null) {
      this.roles.addAll(roles);
    }
  }

  /**
   * This method will assign a list to users member field.
   *
   * @param users List which has to be assigned
   */
  public void setUsers(final Set<String> users) {
    this.users.clear();
    if (users != null) {
      this.users.addAll(users);
    }
  }

}
