package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class to store role names or uuids to be added/removed as role member(s).
 */
@Getter
@Setter
@NoArgsConstructor
public class Roles {

  /**
   * Field to store role names or uuids to be added as role members.
   */
  @SerializedName("Add")
  private final Set<String> keysToAddAsRoleMembers = new HashSet<>();
  /**
   * Field to store role names or uuids to be deleted as role members.
   */
  @SerializedName("Delete")
  private final Set<String> keysToDeleteAsRoleMembers = new HashSet<>();

  /**
   * Method to assign a set of role names or uuids to keysToAddAsRoleMembers member field.
   *
   * @param keysToAddAsRoleMembers Set which has to be assigned
   */
  public void setKeysToAddAsRoleMembers(final Set<String> keysToAddAsRoleMembers) {
    this.keysToAddAsRoleMembers.clear();
    if (keysToAddAsRoleMembers != null) {
      this.keysToAddAsRoleMembers.addAll(keysToAddAsRoleMembers);
    }
  }

  /**
   * Method to assign a set of role names or uuids to uuidsToDeleteAsRoleMembers member field.
   *
   * @param keysToDeleteAsRoleMembers Set which has to be assigned
   */
  public void setKeysToDeleteAsRoleMembers(final Set<String> keysToDeleteAsRoleMembers) {
    this.keysToDeleteAsRoleMembers.clear();
    if (keysToDeleteAsRoleMembers != null) {
      this.keysToDeleteAsRoleMembers.addAll(keysToDeleteAsRoleMembers);
    }
  }
}
