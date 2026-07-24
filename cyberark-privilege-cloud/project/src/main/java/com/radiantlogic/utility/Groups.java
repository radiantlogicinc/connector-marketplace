package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Class to store group UUIDs or names to be deleted from role as member(s).
 */
@Getter
@NoArgsConstructor
public class Groups {

  /**
   * Set to store group UUIDs or names to be deleted from role as member(s).
   */
  @SerializedName("Delete")
  private final Set<String> keysToDeleteAsRoleMembers = new HashSet<>();

  /**
   * Method to assign a set of group uuids or names to keysToDeleteAsRoleMembers member field.
   *
   * @param keysToDeleteAsRoleMembers Group uuids or names Set which has to be assigned
   */
  public void setKeysToDeleteAsRoleMembers(
      final Set<String> keysToDeleteAsRoleMembers) {
    this.keysToDeleteAsRoleMembers.clear();
    if (keysToDeleteAsRoleMembers != null) {
      this.keysToDeleteAsRoleMembers.addAll(keysToDeleteAsRoleMembers);
    }
  }
}
