package com.radiantlogic.exception;

/**
 * Class to define custom exception.
 */
public class ServiceNowException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Constructor to initialize super class's members.
   *
   * @param message Exception message
   * @param cause   Exception cause
   */
  public ServiceNowException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructor to initialize super class's member.
   *
   * @param message Exception message
   */
  public ServiceNowException(final String message) {
    super(message);
  }
}
