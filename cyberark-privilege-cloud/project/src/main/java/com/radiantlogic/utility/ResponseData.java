package com.radiantlogic.utility;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse api response details.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResponseData {

  /**
   * This field will store api response.
   */
  private String apiResponse;
  /**
   * This field will store api response message.
   */
  private String responseMessage;
  /**
   * This method will store api response code.
   */
  private int responseCode;
}
