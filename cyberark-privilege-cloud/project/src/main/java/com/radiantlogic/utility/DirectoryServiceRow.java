package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store various directory service attributes.
 */
@Getter
@Setter
@NoArgsConstructor
public class DirectoryServiceRow {

  /**
   * This field will store service key value.
   */
  @SerializedName("Service")
  private String service;
  /**
   * This field will store display name key value.
   */
  @SerializedName("DisplayName")
  private String displayName;
  /**
   * This field will store tenant key value.
   */
  @SerializedName("Tenant")
  private String tenant;
  /**
   * This field will store name key value.
   */
  @SerializedName("Name")
  private String name;
  /**
   * This field will store status key value.
   */
  @SerializedName("Status")
  private String status;
  /**
   * This field will store config key value.
   */
  @SerializedName("Config")
  private String config;
  /**
   * This field will store Sync Rules Configured key value.
   */
  @SerializedName("SyncRulesConfigured")
  private Boolean syncRulesConfigured;
  /**
   * This field will store status display key value.
   */
  @SerializedName("StatusDisplay")
  private String statusDisplay;
  /**
   * This field will store everybody key value.
   */
  @SerializedName("Everybody")
  private Boolean everybody;
  /**
   * This field will store description key value.
   */
  @SerializedName("Description")
  private String description;
  /**
   * This field will store Directory Service Uuid key value.
   */
  private String directoryServiceUuid;
  /**
   * This field will store Sync job status. CyberArk returns null for this field and there is no
   * official doc where it's type is mentioned, so added with Object type.
   */
  @SerializedName("SyncJobStatus")
  private Object syncJobStatus;
  /**
   * This field will store short display name.
   */
  @SerializedName("DisplayNameShort")
  private String displayNameShort;
}
