package com.radiantlogic.enums;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.model.GroupRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Enum representing supported group attributes.
 *
 * <p>Each constant defines:
 * <ul>
 *   <li>The external attribute name</li>
 *   <li>A getter for extracting the value from {@code GroupRow}</li>
 * </ul>
 */
@Getter
public enum Group {
  NAME("Name", GroupRow::getName),
  DESCRIPTION("Description", GroupRow::getDescription),
  ACTIVE("Active", GroupRow::getActive),
  SYS_ID("SysId", GroupRow::getSysId),
  SOURCE("Source", GroupRow::getSource),
  TYPE("Type", GroupRow::getType),
  INCLUDE_MEMBERS("IncludeMembers", GroupRow::getIncludeMembers),
  SYS_CREATED_BY("SysCreatedBy", GroupRow::getSysCreatedBy),
  GROUP_USERS("GroupUsers", GroupRow::getGroupUsers),
  GROUP_ROLES("GroupRoles", GroupRow::getGroupRoles);

  /**
   * External attribute name used for lookup.
   */
  private final String attributeName;

  /**
   * Getter used to extract attribute value from GroupRow.
   */
  private final Function<GroupRow, Object> getter;

  /**
   * Lookup map for resolving attribute name to enum constant.
   */
  private static final Map<String, Group> LOOKUP =
      Collections.unmodifiableMap(Arrays.stream(values())
          .collect(Collectors.toMap(
              group -> group.attributeName.toLowerCase(Locale.ROOT),
              group -> group)));

  Group(final String attributeName,
        final Function<GroupRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * Extracts the attribute value from the given GroupRow.
   *
   * @param group Group row instance
   * @return Extracted value
   */
  public Object extractValue(final GroupRow group) {
    return getter.apply(group);
  }

  /**
   * Resolves enum constant from attribute name.
   *
   * @param fieldName Attribute name
   * @return Matching group enum constant
   * @throws ServiceNowException If the attribute name is invalid
   */
  public static Group fromName(final String fieldName)
      throws ServiceNowException {
    if (StringUtils.isBlank(fieldName)) {
      throw new ServiceNowException("Attribute name cannot be blank");
    }
    Group group = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (group == null) {
      throw new ServiceNowException(
          String.format("Invalid attribute name: %s", fieldName));
    }
    return group;
  }
}
