package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse safe data.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class SafeRow {

  /**
   * This field will parse safe's number.
   */
  private Long safeNumber;
  /**
   * This field will parse safe's location.
   */
  private String location;
  /**
   * This field will parse safe's olacEnabled boolean value.
   */
  @SerializedName("olacEnabled")
  private Boolean isOlacEnabled;
  /**
   * This field will parse safe's number of versions retention value.
   */
  private Long numberOfVersionsRetention;
  /**
   * This field will parse safe's number of days retention value.
   */
  private Long numberOfDaysRetention;
  /**
   * This field will parse safe's autoPurgeEnabled boolean value.
   */
  @SerializedName("autoPurgeEnabled")
  private Boolean isAutoPurgeEnabled;
  /**
   * This field will parse safe's creation time in epoch.
   */
  private Long creationTime;
  /**
   * This field will parse safe's modification time in epoch.
   */
  private Long lastModificationTime;
  /**
   * This field will parse safe's name.
   */
  private String safeName;
  /**
   * This field will parse safe's description.
   */
  private String description;
  /**
   * This field will parse safe's managing CPM.
   */
  @SerializedName("managingCPM")
  private String managingCpm;
  /**
   * This field will parse safe's isExpiredMember boolean value.
   */
  private Boolean isExpiredMember;
  /**
   * This field will parse safe's creator details.
   */
  @SerializedName("creator")
  private SafeCreator safeCreator;
  /**
   * This field will store safe's formatted create date.
   */
  private String created;
  /**
   * This field will store safe's formatted modified date.
   */
  private String modified;
  /**
   * This field will store safe's members.
   */
  private final List<SafeMember> members = new ArrayList<>();

  /**
   * This method will assign a list to members field.
   *
   * @param members List which has to be assigned
   */
  public void setMembers(List<SafeMember> members) {
    this.members.clear();
    if (members != null) {
      this.members.addAll(members);
    }
  }
}