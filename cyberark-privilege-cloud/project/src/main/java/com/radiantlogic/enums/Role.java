package com.radiantlogic.enums;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import lombok.Getter;

/**
 * This enum will contain role entity related attributes.
 */
@Getter
@SuppressWarnings(PMD_KEY)
public enum Role {
  ID("ID"),
  NAME("Name"),
  ROLE_TYPE("RoleType"),
  DESCRIPTION("Description"),
  MEMBERS("Members"),
  ROLE_RIGHTS("RoleRights"),
  ROLE("role");
  /**
   * This field will store attribute name.
   */
  private final String attributeName;

  Role(final String attributeName) {
    this.attributeName = attributeName;
  }
}