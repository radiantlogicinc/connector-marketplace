package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store directory service record.
 */
@Getter
@Setter
@NoArgsConstructor
public class DirectoryServiceRecord {

  /**
   * This field will store directory service record.
   */
  @SerializedName("Row")
  private DirectoryServiceRow row;
}
