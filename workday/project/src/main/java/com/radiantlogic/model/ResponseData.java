package com.radiantlogic.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;

/**
 * Immutable data holder representing an HTTP response.
 *
 * <p>Encapsulates the HTTP status code and response body returned from an API call.
 * This class is used to standardize response handling across the client.
 */
@Getter
@RequiredArgsConstructor
public final class ResponseData {

  /**
   * Raw API response body returned by the server.
   */
  private final String apiResponse;

  /**
   * Human-readable message associated with the API response, if provided.
   */
  private final String responseMessage;

  /**
   * HTTP status code returned by the API.
   */
  private final int responseCode;

  /**
   * HTTP response headers returned by the server.
   */
  private final Headers headers;
}
