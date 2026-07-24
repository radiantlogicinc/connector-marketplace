package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse each row of fetched record.
 */
@Getter
@Setter
@NoArgsConstructor
public class Record {

  /**
   * This object will parse row data.
   */
  @SerializedName("Row")
  private RecordRow recordRow;

  /**
   * List to store role rights.
   */
  private final List<RoleRight> roleRights = new ArrayList<>();

  /**
   * Method to assign role rights to the field named roleRights.
   *
   * @param roleRights List of role rights to be assigned
   */
  public void setRoleRights(final List<RoleRight> roleRights) {
    this.roleRights.clear();
    if (roleRights != null) {
      this.roleRights.addAll(roleRights);
    }
  }
}
