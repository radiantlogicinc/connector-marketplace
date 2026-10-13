package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic response model for membership creation API calls (user-group, user-role, etc.).
 */
@Getter
@Setter
@NoArgsConstructor
public class MembershipCreationResponse {

  /**
   * Result object containing the sys_id of the created membership.
   */
  private MembershipResult result;

  /**
   * Inner class representing the result of a membership creation operation.
   */
  @Getter
  @Setter
  @NoArgsConstructor
  public static class MembershipResult {

    /**
     * System ID of the created membership.
     */
    @SerializedName("sys_id")
    private String sysId;
  }
}
