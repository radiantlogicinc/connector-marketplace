package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse access token api response.
 */
@Getter
@Setter
@NoArgsConstructor
public class AccessToken {

  /**
   * This field will parse access token's value.
   */
  @SerializedName("access_token")
  private String token;
  /**
   * This field will parse token's expiry value.
   */
  @SerializedName("expires_in")
  private int expiresIn;
}
