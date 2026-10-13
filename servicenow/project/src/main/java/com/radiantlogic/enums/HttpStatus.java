package com.radiantlogic.enums;

import com.radiantlogic.iddm.base.StatusCode;

/**
 * Enum containing status codes used by the connector.
 */
@SuppressWarnings("PMD.ShortVariable")
public enum HttpStatus implements StatusCode {

  OK("OK", 200),
  TOKEN_EXPIRED("Token Expired", 401),
  NO_CONTENT("No Content", 204),
  CREATED("Created", 201);
  /**
   * Field to store status code name.
   */
  private final String name;
  /**
   * Field to store status code value.
   */
  private final int value;
  /**
   * Constant to store api error starting code.
   */
  private static final int API_ERROR_STARTING_CODE = 400;

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
    return this.value >= API_ERROR_STARTING_CODE;
  }
}
