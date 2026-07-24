package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class to store role right details.
 */
@Getter
@Setter
@NoArgsConstructor
public class RoleRight {

  /**
   * Field to store role right description.
   */
  @SerializedName("Description")
  private String description;
  /**
   * Field to store role right scope.
   */
  @SerializedName("Scope")
  private String scope;
  /**
   * Field to store role right service name.
   */
  @SerializedName("ServiceName")
  private String serviceName;
  /**
   * Field to store role right path.
   */
  @SerializedName("Path")
  private String path;
}
