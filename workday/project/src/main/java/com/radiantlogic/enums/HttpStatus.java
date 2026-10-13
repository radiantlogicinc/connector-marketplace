package com.radiantlogic.enums;

import com.radiantlogic.iddm.base.StatusCode;

/**
 * Enum representing standard HTTP status codes.
 *
 * <p>Provides a set of commonly used HTTP response statuses along with their numeric codes
 * and related utility methods for status evaluation.
 */
@SuppressWarnings("PMD.ShortVariable")
public enum HttpStatus implements StatusCode {

  OK("OK", 200),
  TOKEN_EXPIRED("Token Expired", 401),
  NO_CONTENT("No Content", 204),
  CREATED("Created", 201);
  /**
   * The human-readable name of the HTTP status.
   */
  private final String name;
  /**
   * The numeric HTTP status code value.
   */
  private final int value;
  /**
   * The starting HTTP status code for client and server-side errors.
   */
  private static final int API_ERROR_STARTING_CODE = 400;

  /**
   * Constructs an HTTP status instance with the specified status name and numeric value.
   *
   * @param name human-readable name describing the HTTP status
   * @param value numeric HTTP status code value
   */
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
