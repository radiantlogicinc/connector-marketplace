package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for role object.
 */
@Getter
@Setter
@NoArgsConstructor
public class RoleRow {

  /**
   * Stores role grantable status.
   */
  private String grantable;
  /**
   * Stores role description.
   */
  private String description;
  /**
   * Stores role elevated privilege status.
   */
  @SerializedName("elevated_privilege")
  private String elevatedPrivilege;
  /**
   * Stores role suffix.
   */
  private String suffix;
  /**
   * Stores role scoped admin status.
   */
  @SerializedName("scoped_admin")
  private String scopedAdmin;
  /**
   * Stores role 'can delegate' status.
   */
  @SerializedName("can_delegate")
  private String delegateCapability;
  /**
   * Stores role includes roles.
   */
  @SerializedName("includes_roles")
  private String includesRoles;
  /**
   * Stores role system id.
   */
  @SerializedName("sys_id")
  private String sysId;
  /**
   * Stores role federated id.
   */
  @SerializedName("federated_id")
  private String federatedId;
  /**
   * Stores role name.
   */
  private String name;
  /**
   * Stores role system created by.
   */
  @SerializedName("sys_created_by")
  private String sysCreatedBy;
  /**
   * Stores role system policy.
   */
  @SerializedName("sys_policy")
  private String sysPolicy;
  /**
   * Stores child role names.
   */
  private List<String> childRoles;
}
