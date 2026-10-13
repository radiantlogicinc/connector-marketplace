package com.radiantlogic.enums;

import static com.radiantlogic.utility.Constants.NAME_ATTRIBUTE_VALUE_CHANGE_ERROR;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.model.UserRow;
import com.radiantlogic.utility.Util;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Enum representing supported user attributes.
 *
 * <p>Each constant defines:
 * <ul>
 *   <li>The external attribute name</li>
 *   <li>A getter for extracting the value from {@code UserRow}</li>
 *   <li>Custom update logic for applying modifications</li>
 * </ul>
 *
 * <p>This enum encapsulates attribute-specific validation and update behavior,
 * eliminating the need for switch-based processing.
 */
@Getter
public enum User {
  COUNTRY("Country", UserRow::getCountry,
      (row, value, type, attr) -> row.setCountry(value)),
  SOURCE("Source", UserRow::getSource,
      (row, value, type, attr) -> row.setSource(value)),
  STATE("State", UserRow::getState,
      (row, value, type, attr) -> row.setState(value)),
  ZIP("Zip", UserRow::getZip,
      (row, value, type, attr) -> row.setZip(value)),
  PHONE("Phone", UserRow::getPhone,
      (row, value, type, attr) -> row.setPhone(value)),
  EMPLOYEE_NUMBER("EmployeeNumber", UserRow::getEmployeeNumber,
      (row, value, type, attr) -> row.setEmployeeNumber(value)),
  GENDER("Gender", UserRow::getGender,
      (row, value, type, attr) -> row.setGender(value)),
  CITY("City", UserRow::getCity,
      (row, value, type, attr) -> row.setCity(value)),
  TITLE("Title", UserRow::getTitle,
      (row, value, type, attr) -> row.setTitle(value)),
  STREET("Street", UserRow::getStreet,
      (row, value, type, attr) -> row.setStreet(value)),
  FIRST_NAME("FirstName", UserRow::getFirstName,
      (row, value, type, attr) -> row.setFirstName(value)),
  INTRODUCTION("Introduction", UserRow::getIntroduction,
      (row, value, type, attr) -> row.setIntroduction(value)),
  LAST_NAME("LastName", UserRow::getLastName,
      (row, value, type, attr) -> row.setLastName(value)),
  MIDDLE_NAME("MiddleName", UserRow::getMiddleName,
      (row, value, type, attr) -> row.setMiddleName(value)),
  MOBILE_PHONE("MobilePhone", UserRow::getMobilePhone,
      (row, value, type, attr) -> row.setMobilePhone(value)),
  EMAIL("Email", UserRow::getEmail,
      (row, value, type, attr) -> row.setEmail(value)),
  IDENTITY_TYPE("IdentityType", UserRow::getIdentityType,
      (row, value, type, attr) -> row.setIdentityType(value)),
  /**
   * Username attribute for the user record.
   *
   * <p>Existing records are assumed to always have a username set, therefore
   * this attribute is treated as immutable and cannot be updated. It can only be provided when
   * creating a new user. As update username request can come as ADD modification type using cli,
   * checkWhetherUserNameAttribute in UpdateUserRequestHandler class will restrict the same
   * considering username as immutable.
   */
  USER_NAME("UserName", UserRow::getUsername,
      (row, value, type, attr) -> {
        Util.validateOnlyAdd(type, "username can only be added, not updated");
        Util.validateNotBlank(value, "username cannot be blank");
        row.setUsername(value);
      }),
  /**
   * Active flag must be a valid boolean string and cannot be deleted.
   */
  ACTIVE("Active", UserRow::getActive,
      (row, value, type, attr) -> {
        Util.validateNotDelete(type, "active status cannot be deleted");
        row.setActive(Util.validateAndNormalizeBoolean(value));
      }),
  /**
   * Locked-out flag must be a valid boolean string and cannot be deleted.
   */
  LOCKED_OUT("LockedOut", UserRow::getLockedOut,
      (row, value, type, attr) -> {
        Util.validateNotDelete(type, "locked out status cannot be deleted");
        row.setLockedOut(Util.validateAndNormalizeBoolean(value));
      }),
  /**
   * Internal integration flag must be a valid boolean string and cannot be deleted.
   */
  INTERNAL_INTEGRATION_USER("InternalIntegrationUser",
      UserRow::getInternalIntegrationUser,
      (row, value, type, attr) -> {
        Util.validateNotDelete(type,
            "internal integration user status cannot be deleted");
        row.setInternalIntegrationUser(Util.validateAndNormalizeBoolean(value));
      }),
  /**
   * Name is system-derived based upon first & last name and cannot be directly modified.
   */
  NAME("Name", UserRow::getName,
      (row, value, type, attr) -> {
        throw new ServiceNowException(
            NAME_ATTRIBUTE_VALUE_CHANGE_ERROR);
      }),
  /**
   * System identifier cannot be modified.
   */
  SYS_ID("SysId", UserRow::getSysId,
      (row, value, type, attr) -> {
        throw new ServiceNowException(
            String.format("'%s' attribute cannot be added/modified/deleted",
                attr.getName()));
      }),
  /**
   * Federated identifier cannot be modified.
   */
  FEDERATED_ID("FederatedId", UserRow::getFederatedId,
      (row, value, type, attr) -> {
        throw new ServiceNowException(
            String.format("'%s' attribute cannot be added/modified/deleted",
                attr.getName()));
      }),
  /**
   * User roles attribute.
   *
   * <p><b>IMPORTANT:</b> Role modifications are handled via
   * {@link com.radiantlogic.update.UserRoleRelationshipManager}, NOT through this enum's
   * {@code apply()} path. The dispatch logic in
   * {@link com.radiantlogic.update.UpdateUserRequestHandler} explicitly checks for USER_ROLES and
   * routes ADD/REPLACE/DELETE operations to UserRoleRelationshipManager instead of calling this
   * enum's handler.
   *
   * <p><b>Defensive Fallback:</b> The lambda below serves as a safety net in case the dispatch
   * logic changes or is bypassed. Under normal operation, this code path is unreachable because
   * USER_ROLES modifications are intercepted before reaching this handler. If this exception is
   * ever thrown, it indicates a bug in the dispatch logic.
   */
  USER_ROLES("UserRoles", UserRow::getUserRoles,
      (row, value, type, attr) -> {
        throw new ServiceNowException(
            String.format("'%s' attribute cannot be added/modified/deleted",
                attr.getName()));
      });

  /**
   * External attribute name used for lookup.
   */
  private final String attributeName;

  /**
   * Getter used to extract attribute value from UserRow.
   */
  private final Function<UserRow, Object> getter;

  /**
   * Handler containing attribute-specific update logic.
   */
  private final AttributeHandler handler;

  /**
   * Lookup map for resolving attribute name to enum constant.
   */
  private static final Map<String, User> LOOKUP =
      Collections.unmodifiableMap(Arrays.stream(values())
          .collect(Collectors.toMap(
              user -> user.attributeName.toLowerCase(Locale.ROOT),
              user -> user)));

  /**
   * Functional interface defining attribute update behavior.
   */
  @FunctionalInterface
  private interface AttributeHandler {

    /**
     * Applies attribute update logic.
     *
     * @param userRow          User row to update
     * @param value            Attribute value
     * @param modificationType Modification type
     * @param attribute        Original attribute metadata
     * @throws ServiceNowException If validation fails
     */
    void apply(UserRow userRow,
               String value,
               Modification.ModificationType modificationType,
               Attribute attribute)
        throws ServiceNowException;
  }

  User(final String attributeName,
       final Function<UserRow, Object> getter,
       final AttributeHandler handler) {
    this.attributeName = attributeName;
    this.getter = getter;
    this.handler = handler;
  }

  /**
   * Extracts the attribute value from the given UserRow.
   *
   * @param user User row instance
   * @return Extracted value
   */
  public Object extractValue(final UserRow user) {
    return getter.apply(user);
  }

  /**
   * Applies attribute update logic to the provided UserRow.
   *
   * @param userRow          Target user row
   * @param value            Attribute value
   * @param modificationType Modification type
   * @param attribute        Original attribute metadata
   * @throws ServiceNowException If validation fails
   */
  public void apply(final UserRow userRow,
                    final String value,
                    final Modification.ModificationType modificationType,
                    final Attribute attribute)
      throws ServiceNowException {
    handler.apply(userRow, value, modificationType, attribute);
  }

  /**
   * Resolves enum constant from attribute name.
   *
   * @param fieldName Attribute name
   * @return Matching user enum constant
   * @throws ServiceNowException If the attribute name is invalid
   */
  public static User fromName(final String fieldName)
      throws ServiceNowException {
    if (StringUtils.isBlank(fieldName)) {
      throw new ServiceNowException("Attribute name cannot be blank");
    }
    User user = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (user == null) {
      throw new ServiceNowException(
          String.format("Invalid attribute name: %s", fieldName));
    }
    return user;
  }

  /**
   * Resolves the enum constant from the given attribute name if present.
   *
   * @param fieldName Attribute name
   * @return Optional containing the matching {@link User} enum constant, or empty if the name is
   *         blank or not found
   */
  public static Optional<User> fromNameOptional(final String fieldName) {
    if (StringUtils.isBlank(fieldName)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        LOOKUP.get(fieldName.toLowerCase(Locale.ROOT))
    );
  }
}
