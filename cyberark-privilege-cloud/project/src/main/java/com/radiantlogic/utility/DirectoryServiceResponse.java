package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store Directory Services api response.
 */
@Getter
@Setter
@NoArgsConstructor
public class DirectoryServiceResponse {

  /**
   * This field will store whether result was successful or not.
   */
  @SerializedName("success")
  private boolean success;
  /**
   * This field will store directory services data.
   */
  @SerializedName("Result")
  private DirectoryServicesResult result;
}
