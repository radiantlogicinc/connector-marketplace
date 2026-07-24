package com.radiantlogic.utility;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store remote machines access details for an account.
 */
@Getter
@Setter
@NoArgsConstructor
public class RemoteMachinesAccess {

  /**
   * This field will store comma separated machines.
   */
  private String remoteMachines;
  /**
   * This field will store Boolean whether access restricted to remote machines.
   */
  private Boolean accessRestrictedToRemoteMachines;

}
