package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic response model for membership query API calls (user-group, user-role, etc.).
 */
@Getter
@Setter
@NoArgsConstructor
public class MembershipQueryResponse {

  /**
   * List of membership records.
   */
  private List<MembershipRecord> result;

  /**
   * Inner class representing a single membership record.
   */
  @Getter
  @Setter
  @NoArgsConstructor
  public static class MembershipRecord {

    /**
     * System ID of the membership.
     */
    @SerializedName("sys_id")
    private String sysId;
  }
}
