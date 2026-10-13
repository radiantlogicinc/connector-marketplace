package com.radiantlogic.enums;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.model.RoleRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Enum representing supported role attributes.
 *
 * <p>Each constant defines:
 * <ul>
 *   <li>The external attribute name</li>
 *   <li>A getter for extracting the value from {@code RoleRow}</li>
 * </ul>
 */
@Getter
public enum Role {
  NAME("Name", RoleRow::getName),
  DESCRIPTION("Description", RoleRow::getDescription),
  SYS_ID("SysId", RoleRow::getSysId),
  GRANTABLE("Grantable", RoleRow::getGrantable),
  ELEVATED_PRIVILEGE("ElevatedPrivilege", RoleRow::getElevatedPrivilege),
  SUFFIX("Suffix", RoleRow::getSuffix),
  SCOPED_ADMIN("ScopedAdmin", RoleRow::getScopedAdmin),
  CAN_DELEGATE("CanDelegate", RoleRow::getDelegateCapability),
  INCLUDES_ROLES("IncludesRoles", RoleRow::getIncludesRoles),
  FEDERATED_ID("FederatedId", RoleRow::getFederatedId),
  SYS_CREATED_BY("SysCreatedBy", RoleRow::getSysCreatedBy),
  SYS_POLICY("SysPolicy", RoleRow::getSysPolicy),
  CHILD_ROLES("ChildRoles", RoleRow::getChildRoles);

  /**
   * External attribute name used for lookup.
   */
  private final String attributeName;

  /**
   * Getter used to extract attribute value from RoleRow.
   */
  private final Function<RoleRow, Object> getter;

  /**
   * Lookup map for resolving attribute name to enum constant.
   */
  private static final Map<String, Role> LOOKUP =
      Collections.unmodifiableMap(Arrays.stream(values())
          .collect(Collectors.toMap(
              role -> role.attributeName.toLowerCase(Locale.ROOT),
              role -> role)));

  Role(final String attributeName,
       final Function<RoleRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the attribute value from the given RoleRow.
   *
   * @param role Role row instance
   * @return Extracted value
   */
  public Object extractValue(final RoleRow role) {
    return getter.apply(role);
  }

  /**
   * Resolves enum constant from attribute name.
   *
   * @param fieldName Attribute name
   * @return Matching role enum constant
   * @throws ServiceNowException If the attribute name is invalid
   */
  public static Role fromName(final String fieldName)
      throws ServiceNowException {
    if (StringUtils.isBlank(fieldName)) {
      throw new ServiceNowException("Attribute name cannot be blank");
    }
    Role role = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (role == null) {
      throw new ServiceNowException(
          String.format("Invalid attribute name: %s", fieldName));
    }
    return role;
  }
}
