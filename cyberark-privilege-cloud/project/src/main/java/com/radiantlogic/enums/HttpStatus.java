package com.radiantlogic.enums;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.radiantlogic.iddm.base.StatusCode;

/**
 * This enum will contain status codes used by the connector.
 */
@SuppressWarnings(PMD_KEY)
public enum HttpStatus implements StatusCode {

  OK("OK", 200),
  TOKEN_EXPIRED("Token Expired", 401);
  /**
   * This field will contain status code name.
   */
  private final String name;
  /**
   * This field will contain status code value.
   */
  private final int value;

  HttpStatus(final String name, final int value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String asString() {
    return this.name;
  }

  @Override
  public int asInt() {
    return this.value;
  }

  @Override
  public boolean isError() {
    return this.value != OK.asInt();
  }
}
