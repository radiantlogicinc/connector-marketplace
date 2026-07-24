package com.radiantlogic.utility;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store secret management properties for an account.
 */
@Getter
@Setter
@NoArgsConstructor
public class SecretManagement {

  /**
   * This field will store Boolean is automatic management enabled.
   */
  private Boolean automaticManagementEnabled;
  /**
   * This field will store last modified time in epoch. As per official doc, this data should be
   * coming in lastModifiedDateTime key, but in postman coming as mentioned below.
   */
  private Long lastModifiedTime;
  /**
   * This field will store reason for disabling automatic secret management.
   */
  private String manualManagementReason;
  /**
   * This field will store account management status.
   */
  private String status;
  /**
   * This field will store formatted last modified date time.
   */
  private String lastModifiedDateTime;
}
