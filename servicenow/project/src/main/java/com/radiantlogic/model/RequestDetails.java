package com.radiantlogic.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class to store unique key for request and whether request is for which object.
 */
@Getter
@Setter
@NoArgsConstructor
public class RequestDetails {

  /**
   * Field to store unique key of a record on which operation to be performed.
   */
  private String uniqueKey;
  /**
   * Field to store whether request is for user entity.
   */
  private boolean userRequest;
  /**
   * Field to store whether request is for group entity.
   */
  private boolean groupRequest;
  /**
   * Field to store whether request is for role entity.
   */
  private boolean roleRequest;
}

