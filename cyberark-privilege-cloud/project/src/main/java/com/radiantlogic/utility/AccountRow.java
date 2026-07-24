package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store account details.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class AccountRow {

  /**
   * This field will store account's category modification time.
   */
  private Long categoryModificationTime;
  /**
   * This field will store account's platform id.
   */
  private String platformId;
  /**
   * This field will store safe name to which account belongs to.
   */
  private String safeName;
  /**
   * This field will store account's id.
   */
  private String id;
  /**
   * This field will store account's name.
   */
  private String name;
  /**
   * This field will store account's address.
   */
  private String address;
  /**
   * This field will store account's Username.
   */
  private String userName;
  /**
   * This field will store account's secret type.
   */
  private String secretType;
  /**
   * This field will store account's created time in epoch.
   */
  private Long createdTime;
  /**
   * This field will store account's deletion time in epoch.
   */
  private Long deletionTime;
  /**
   * This field will store account's secret management properties.
   */
  private SecretManagement secretManagement;
  /**
   * This field will store account's platform account properties. As properties may differ for
   * different accounts and to avoid data loss when additional properties will be added, field has
   * been defined as a map of objects instead of a separate pojo.
   */
  private Map<String, Object> platformAccountProperties;
  /**
   * This field will store account's remote machine access properties.
   */
  private RemoteMachinesAccess remoteMachinesAccess;
  /**
   * This field will store account's formatted category modification time.
   */
  private String formattedCategoryModificationTime;
  /**
   * This field will store account's formatted created time.
   */
  private String formattedCreatedTime;
}
