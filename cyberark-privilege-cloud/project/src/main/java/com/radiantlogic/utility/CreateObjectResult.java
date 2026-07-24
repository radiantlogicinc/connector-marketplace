package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse create user api response.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class CreateObjectResult {

  /**
   * This field will parse boolean value true or false for create operation.
   */
  private boolean success;
  /**
   * This field will parse api response message.
   */
  @SerializedName("Message")
  private String message;
  /**
   * This field will parse created user's id.
   */
  @SerializedName("Result")
  private String id;
}
