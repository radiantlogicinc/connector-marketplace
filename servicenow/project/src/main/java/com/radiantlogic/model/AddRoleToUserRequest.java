package com.radiantlogic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for add role to user request payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddRoleToUserRequest {

  /**
   * User name.
   */
  private String user;

  /**
   * Role name.
   */
  private String role;
}
