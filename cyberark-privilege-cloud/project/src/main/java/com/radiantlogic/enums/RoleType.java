package com.radiantlogic.enums;

import lombok.Getter;

/**
 * This enum will contain allowed role type values.
 */
@Getter
public enum RoleType {
  PRINCIPAL_LIST("PrincipalList"),
  EVERYBODY("Everybody"),
  SCRIPT("Script");
  /**
   * This field will store role type name.
   */
  private final String attributeName;

  RoleType(final String attributeName) {
    this.attributeName = attributeName;
  }
}