package com.radiantlogic.enums;

import lombok.Getter;

/**
 * This enum will store role member's types.
 */
@Getter
public enum RoleMemberType {
  USER("(User)", "User"),
  ROLE("(Role)", "Role"),
  GROUP("(Group)", "Group");
  /**
   * This field will store member type key.
   */
  private final String memberTypeKey;
  /**
   * This field will store member type value.
   */
  private final String memberTypeValue;

  RoleMemberType(final String memberTypeKey, final String memberTypeValue) {
    this.memberTypeKey = memberTypeKey;
    this.memberTypeValue = memberTypeValue;
  }
}