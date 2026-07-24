package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse safe member's data.
 */
@Getter
@Setter
@NoArgsConstructor
public class SafeMember {

  /**
   * This field will parse safe member's id.
   */
  private String memberId;
  /**
   * This field will parse safe member's name.
   */
  private String memberName;
  /**
   * This field will parse safe member's type.
   */
  private String memberType;
  /**
   * This field will parse safe member's membership expiration date. As per official doc, its type
   * should be Boolean, but date was coming in epoch
   */
  private Long membershipExpirationDate;
  /**
   * This field will store safe member's formatted membership expiry.
   */
  private String membershipExpiry;
  /**
   * This field will parse safe member's isExpiredMembershipEnable boolean value.
   */
  private Boolean isExpiredMembershipEnable;
  /**
   * This field will parse safe member's isPredefinedUser boolean value.
   */
  private Boolean isPredefinedUser;
  /**
   * This field will parse safe member's isReadOnly boolean value.
   */
  private Boolean isReadOnly;
  /**
   * This field will parse safe member's permissions.
   */
  @SerializedName("permissions")
  private SafeMemberPermissions safeMemberPermissions;
}