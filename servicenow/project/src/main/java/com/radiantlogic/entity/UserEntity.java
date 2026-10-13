package com.radiantlogic.entity;

import com.radiantlogic.iddm.schema.Attribute;
import com.radiantlogic.iddm.schema.Entity;
import java.util.List;
import lombok.NoArgsConstructor;

/**
 * Class to define user entity with attributes.
 */
@Entity(name = "user", namingKey = "user")
@NoArgsConstructor
@SuppressWarnings("PMD.TooManyFields")
public class UserEntity {

  /**
   * Field mapped to sysid attribute.
   */
  @Attribute()
  private String sysId;
  /**
   * Field mapped to country attribute.
   */
  @Attribute(isNullable = true)
  private String country;
  /**
   * Field mapped to source attribute.
   */
  @Attribute(isNullable = true)
  private String source;
  /**
   * Field mapped to state attribute.
   */
  @Attribute(isNullable = true)
  private String state;
  /**
   * Field mapped to identitytype attribute.
   */
  @Attribute(isNullable = true)
  private String identityType;
  /**
   * Field mapped to zip attribute.
   */
  @Attribute(isNullable = true)
  private String zip;
  /**
   * Field mapped to active attribute.
   */
  @Attribute(isNullable = true)
  private String active;
  /**
   * Field mapped to phone attribute.
   */
  @Attribute(isNullable = true)
  private String phone;
  /**
   * Field mapped to name attribute.
   */
  @Attribute(isNullable = true)
  private String name;
  /**
   * Field mapped to employeenumber attribute.
   */
  @Attribute(isNullable = true)
  private String employeeNumber;
  /**
   * Field mapped to gender attribute.
   */
  @Attribute(isNullable = true)
  private String gender;
  /**
   * Field mapped to city attribute.
   */
  @Attribute(isNullable = true)
  private String city;
  /**
   * Field mapped to username attribute.
   */
  @Attribute(isNamingAttribute = true)
  private String userName;
  /**
   * Field mapped to title attribute.
   */
  @Attribute(isNullable = true)
  private String title;
  /**
   * Field mapped to federatedid attribute.
   */
  @Attribute(isNullable = true)
  private String federatedId;
  /**
   * Field mapped to mobilephone attribute.
   */
  @Attribute(isNullable = true)
  private String mobilePhone;
  /**
   * Field mapped to street attribute.
   */
  @Attribute(isNullable = true)
  private String street;
  /**
   * Field mapped to firstname attribute.
   */
  @Attribute(isNullable = true)
  private String firstName;
  /**
   * Field mapped to introduction attribute.
   */
  @Attribute(isNullable = true)
  private String introduction;
  /**
   * Field mapped to lockedout attribute.
   */
  @Attribute(isNullable = true)
  private String lockedOut;
  /**
   * Field mapped to lastname attribute.
   */
  @Attribute(isNullable = true)
  private String lastName;
  /**
   * Field mapped to middlename attribute.
   */
  @Attribute(isNullable = true)
  private String middleName;
  /**
   * Field mapped to internalintegrationuser attribute.
   */
  @Attribute(isNullable = true)
  private String internalIntegrationUser;
  /**
   * Field mapped to email attribute.
   */
  @Attribute(isNullable = true)
  private String email;
  /**
   * Mapped to userRoles attribute.
   */
  @Attribute(isNullable = true)
  private List<String> userRoles;
}
