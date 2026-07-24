package com.radiantlogic.enums;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import lombok.Getter;

/**
 * This enum will contain attributes that can be part of user entity.
 */
@Getter
@SuppressWarnings(PMD_KEY)
public enum User {
  NAME("Name"),
  DISPLAY_NAME("DisplayName"),
  ID("ID"),
  RISK_LEVEL_RANK("RiskLevelRank"),
  STATUS("Status"),
  MOBILE("MobileNumber"),
  MAIL("Mail"),
  SEND_EMAIL_INVITE("SendEmailInvite"),
  SEND_SMS_INVITE("SendSmsInvite"),
  SOURCE("Source"),
  USER("user");
  /**
   * This field will store attribute name.
   */
  private final String attributeName;

  User(final String attributeName) {
    this.attributeName = attributeName;
  }
}