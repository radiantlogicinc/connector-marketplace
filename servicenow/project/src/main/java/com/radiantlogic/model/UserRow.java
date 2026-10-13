package com.radiantlogic.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POJO class for user object.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.TooManyFields")
public class UserRow {

  /**
   * Field to store user country.
   */
  private String country;
  /**
   * Field to store user source.
   */
  private String source;
  /**
   * Field to store user state.
   */
  private String state;
  /**
   * Field to store user identity type.
   */
  @SerializedName("identity_type")
  private String identityType;
  /**
   * Field to store user zip code.
   */
  private String zip;
  /**
   * Field to store user active status.
   */
  private String active;
  /**
   * Field to store user business phone.
   */
  private String phone;
  /**
   * Field to store user full name (will not include middle name).
   */
  private String name;
  /**
   * Field to store user employee number.
   */
  @SerializedName("employee_number")
  private String employeeNumber;
  /**
   * Field to store user gender.
   */
  private String gender;
  /**
   * Field to store user city.
   */
  private String city;
  /**
   * Field to store username.
   */
  @SerializedName("user_name")
  private String username;
  /**
   * Field to store user designation.
   */
  private String title;
  /**
   * Field to store user system id.
   */
  @SerializedName("sys_id")
  private String sysId;
  /**
   * Field to store user federated id.
   */
  @SerializedName("federated_id")
  private String federatedId;
  /**
   * Field to store user mobile no.
   */
  @SerializedName("mobile_phone")
  private String mobilePhone;
  /**
   * Field to store user street.
   */
  private String street;
  /**
   * Field to store user first name.
   */
  @SerializedName("first_name")
  private String firstName;
  /**
   * Field to store user email.
   */
  private String email;
  /**
   * Field to store user - name prefix.
   */
  private String introduction;
  /**
   * Field to store user locked out status.
   */
  @SerializedName("locked_out")
  private String lockedOut;
  /**
   * Field to store user last name.
   */
  @SerializedName("last_name")
  private String lastName;
  /**
   * Field to store user middle name.
   */
  @SerializedName("middle_name")
  private String middleName;
  /**
   * Field to store whether user is internal integration user.
   */
  @SerializedName("internal_integration_user")
  private String internalIntegrationUser;
  /**
   * Field to store user roles.
   */
  private List<String> userRoles;
}
