package com.radiantlogic.utility;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse unique key for request and whether request is for user/role entity.
 */
@Getter
@Setter
@NoArgsConstructor
public class RequestDetails {

  /**
   * This field will store unique key of a record on which operation to be performed.
   */
  private String uniqueKey;
  /**
   * This field will store whether request is for user entity.
   */
  private boolean isUserRequest;
  /**
   * This field will store whether request is for role entity.
   */
  private boolean isRoleRequest;
}
