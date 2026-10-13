package com.radiantlogic.exception;

/**
 * Exception thrown to indicate errors occurring during Workday operations.
 *
 * <p>This is a checked exception used to represent failures such as API errors,
 * request processing issues, or unexpected responses from Workday services.
 */
public class WorkdayException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new WorkdayException with the specified detail message.
   *
   * @param message the detail message describing the reason for the exception
   */
  public WorkdayException(final String message) {
    super(message);
  }

  /**
   * Constructor to initialize super class's members.
   *
   * @param message Exception message
   * @param cause   Exception cause
   */
  public WorkdayException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
