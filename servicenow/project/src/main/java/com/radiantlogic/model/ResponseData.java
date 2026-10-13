package com.radiantlogic.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;

/**
 * Class to store api response data.
 */
@Getter
@RequiredArgsConstructor
public final class ResponseData {

  /**
   * Field to store api response.
   */
  private final String apiResponse;
  /**
   * Field to store api response message.
   */
  private final String responseMessage;
  /**
   * Field to store api response code.
   */
  private final int responseCode;
  /**
   * Field to store response headers.
   */
  private final Headers headers;
}
