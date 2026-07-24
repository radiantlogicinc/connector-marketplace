package com.radiantlogic.exception;

/**
 * This class will be used to raise custom exception.
 */
public class CyberArkPrivilegeCloudException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * This constructor will initialize super class's members.
   *
   * @param message Exception message
   * @param cause   Exception cause
   */
  public CyberArkPrivilegeCloudException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * This constructor will initialize super class's member.
   *
   * @param message Exception message
   */
  public CyberArkPrivilegeCloudException(final String message) {
    super(message);
  }
}
