package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse api response.
 */
@Getter
@Setter
@NoArgsConstructor
public class CyberArkResponse {

  /**
   * This field will parse boolean true or false for success of api operation performed.
   */
  private boolean success;
  /**
   * This field will parse api response message.
   */
  @SerializedName("Message")
  private String message;
  /**
   * This field will parse api response data.
   */
  @SerializedName("Result")
  private ResultData resultData;
  /**
   * This field will parse fetched safes.
   */
  @SerializedName("value")
  private final List<SafeRow> safes = new ArrayList<>();
  /**
   * This field will parse get safes pagination link.
   */
  private String nextLink;

  /**
   * This method will assign a list to safes member field.
   *
   * @param safes List which has to be assigned
   */
  public void setSafes(final List<SafeRow> safes) {
    this.safes.clear();
    if (safes != null) {
      this.safes.addAll(safes);
    }
  }
}
