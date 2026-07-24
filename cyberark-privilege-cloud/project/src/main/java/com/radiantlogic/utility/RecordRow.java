package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse various attributes data.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class RecordRow {

  /**
   * This field will parse user display name.
   */
  @SerializedName("DisplayName")
  private String displayName;
  /**
   * This field will parse user username.
   */
  @SerializedName("Username")
  private String username;
  /**
   * This field will parse user status.
   */
  @SerializedName("Status")
  private String status;
  /**
   * This field will parse object's uuid.
   */
  @SerializedName(value = "ID", alternate = {"Id", "Guid"})
  private String id;
  /**
   * This field will parse user risk level rank.
   */
  @SerializedName("RiskLevelRank")
  private Integer riskLevelRank;
  /**
   * This field will parse user mobile number.
   */
  @SerializedName("MobileNumber")
  private String mobile;
  /**
   * This field will parse user email.
   */
  @SerializedName("Email")
  private String email;
  /**
   * This field will store mail of user to be updated/created.
   */
  @SerializedName("Mail")
  private String mail;
  /**
   * This field will parse object's name/id.
   */
  @SerializedName("Name")
  private String name;
  /**
   * Field to store role/role right description.
   */
  @SerializedName("Description")
  private String description;
  /**
   * This field will parse role members in json string format.
   */
  @SerializedName("Members")
  private String membersStringType;
  /**
   * This field will parse role members.
   */
  private final List<String> members = new ArrayList<>();
  /**
   * This field will parse role member's type.
   */
  @SerializedName("Type")
  private String memberType;
  /**
   * This field will parse role type.
   */
  @SerializedName("RoleType")
  private String roleType;
  /**
   * This field will store role's user members to be added/deleted.
   */
  @SerializedName("Users")
  private Users users;
  /**
   * This field will store role's role members to be added/deleted.
   */
  @SerializedName("Roles")
  private Roles roles;
  /**
   * This field will parse whether email invite to be sent while user creation.
   */
  @SerializedName("SendEmailInvite")
  private Boolean sendEmailInvite;
  /**
   * This field will parse whether sms invite to be sent while user creation.
   */
  @SerializedName("SendSmsInvite")
  private Boolean sendSmsInvite;
  /**
   * This field will parse user source.
   */
  @SerializedName("SourceDsLocalized")
  private String source;
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
  /**
   * Field to store role's group members to be deleted.
   */
  @SerializedName("Groups")
  private Groups groups;

  /**
   * This method will assign a list to members field.
   *
   * @param members List which has to be assigned
   */
  public void setMembers(final List<String> members) {
    this.members.clear();
    if (members != null) {
      this.members.addAll(members);
    }
  }

}
