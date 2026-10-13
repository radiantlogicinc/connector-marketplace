package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an OAuth access token response returned by the authentication service.
 *
 * <p>This class holds token-related information such as the access token value and token type,
 * and is used for deserializing authentication responses.
 */
@Getter
@Setter
@NoArgsConstructor
public class AccessToken {

  /**
   * The access token value used for authentication in API requests.
   */
  @SerializedName("access_token")
  private String token;
  /**
   * The type of token issued.
   */
  @SerializedName("token_type")
  private String tokenType;

}
