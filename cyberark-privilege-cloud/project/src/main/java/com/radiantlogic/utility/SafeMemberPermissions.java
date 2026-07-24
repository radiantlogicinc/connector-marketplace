package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse safe member's permissions.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class SafeMemberPermissions {

  /**
   * This field will parse safe member's useAccounts value.
   */
  @SerializedName("useAccounts")
  private Boolean isUseAccounts;
  /**
   * This field will parse safe member's retrieveAccounts value.
   */
  @SerializedName("retrieveAccounts")
  private Boolean isRetrieveAccounts;
  /**
   * This field will parse safe member's listAccounts value.
   */
  @SerializedName("listAccounts")
  private Boolean isListAccounts;
  /**
   * This field will parse safe member's addAccounts value.
   */
  @SerializedName("addAccounts")
  private Boolean isAddAccounts;
  /**
   * This field will parse safe member's updateAccountContent value.
   */
  @SerializedName("updateAccountContent")
  private Boolean isUpdateAccountContent;
  /**
   * This field will parse safe member's updateAccountProperties value.
   */
  @SerializedName("updateAccountProperties")
  private Boolean isUpdateAccountProperties;
  /**
   * This field will parse safe member's initiateCPMAccountManagementOperations value.
   */
  @SerializedName("initiateCPMAccountManagementOperations")
  private Boolean isInitiateCpmAccountManagementOperations;
  /**
   * This field will parse safe member's specifyNextAccountContent value.
   */
  @SerializedName("specifyNextAccountContent")
  private Boolean isSpecifyNextAccountContent;
  /**
   * This field will parse safe member's renameAccounts value.
   */
  @SerializedName("renameAccounts")
  private Boolean isRenameAccounts;
  /**
   * This field will parse safe member's deleteAccounts value.
   */
  @SerializedName("deleteAccounts")
  private Boolean isDeleteAccounts;
  /**
   * This field will parse safe member's unlockAccounts value.
   */
  @SerializedName("unlockAccounts")
  private Boolean isUnlockAccounts;
  /**
   * This field will parse safe member's manageSafe value.
   */
  @SerializedName("manageSafe")
  private Boolean isManageSafe;
  /**
   * This field will parse safe member's manageSafeMembers value.
   */
  @SerializedName("manageSafeMembers")
  private Boolean isManageSafeMembers;
  /**
   * This field will parse safe member's backupSafe value.
   */
  @SerializedName("backupSafe")
  private Boolean isBackupSafe;
  /**
   * This field will parse safe member's viewAuditLog value.
   */
  @SerializedName("viewAuditLog")
  private Boolean isViewAuditLog;
  /**
   * This field will parse safe member's viewSafeMembers value.
   */
  @SerializedName("viewSafeMembers")
  private Boolean isViewSafeMembers;
  /**
   * This field will parse safe member's requestsAuthorizationLevel1 value.
   */
  @SerializedName("requestsAuthorizationLevel1")
  private Boolean isRequestsAuthorizationLevel1;
  /**
   * This field will parse safe member's requestsAuthorizationLevel2 value.
   */
  @SerializedName("requestsAuthorizationLevel2")
  private Boolean isRequestsAuthorizationLevel2;
  /**
   * This field will parse safe member's accessWithoutConfirmation value.
   */
  @SerializedName("accessWithoutConfirmation")
  private Boolean isAccessWithoutConfirmation;
  /**
   * This field will parse safe member's createFolders value.
   */
  @SerializedName("createFolders")
  private Boolean isCreateFolders;
  /**
   * This field will parse safe member's deleteFolders value.
   */
  @SerializedName("deleteFolders")
  private Boolean isDeleteFolders;
  /**
   * This field will parse safe member's moveAccountsAndFolders value.
   */
  @SerializedName("moveAccountsAndFolders")
  private Boolean isMoveAccountsAndFolders;
}