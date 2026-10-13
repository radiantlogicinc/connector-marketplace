package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class to store get access token api response.
 */
@Getter
@Setter
@NoArgsConstructor
public class AccessToken {

  /**
   * Field to store access token's value.
   */
  @SerializedName("access_token")
  private String token;
  /**
   * Field to store token's expiry value in seconds.
   */
  @SerializedName("expires_in")
  private int expiresIn;
}
