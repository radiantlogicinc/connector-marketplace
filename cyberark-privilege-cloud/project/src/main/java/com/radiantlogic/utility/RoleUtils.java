package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.ASTERISK_KEY;
import static com.radiantlogic.utility.Constants.GET_ROLES_PAYLOAD_FOR_FILTERS_WITH_EXACT_VALUE;
import static com.radiantlogic.utility.Constants.GET_ROLES_PAYLOAD_FOR_FILTERS_WITH_LIKE_COMMAND;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.NOT_OPERATOR_KEY;
import static com.radiantlogic.utility.Constants.NULL_KEY;
import static com.radiantlogic.utility.Constants.PERCENTAGE_KEY;
import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;
import com.radiantlogic.enums.Role;
import com.radiantlogic.enums.RoleMemberType;
import com.radiantlogic.enums.RoleType;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.ldap.Attribute;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class will contain role related utility functions.
 */
@Slf4j
@SuppressWarnings(PMD_KEY)
public final class RoleUtils {

  private RoleUtils() {
  }

  /**
   * This method will return to be linked role members uuids.
   *
   * @param attribute Attribute containing values
   * @return Set of member uuids
   * @throws CyberArkPrivilegeCloudException In case value is blank
   */
  @NonNull
  public static Set<String> getMemberUuids(final Attribute attribute)
      throws CyberArkPrivilegeCloudException {
    Type setType = new TypeToken<Set<String>>() {
    }.getType();
    // can be uuid, pasted as an input or json, pasted in an array as input ["uuid1","uuid2"]
    Set<String> allUuids = new HashSet<>();
    for (String value : attribute.getValues()) {
      if (StringUtils.isBlank(value)) {
        throw new CyberArkPrivilegeCloudException("member uuid cannot be blank");
      }
      try {
        Set<String> uuids = GOOGLE_JSON.fromJson(value, setType);
        allUuids.addAll(uuids);
      } catch (JsonSyntaxException e) {
        allUuids.add(value);
      }
    }
    return allUuids.stream().map(String::toLowerCase).collect(Collectors.toSet());
  }

  /**
   * This method will check whether Script role type is having members to be linked.
   *
   * @param roleRow RolePojo containing required data
   * @throws CyberArkPrivilegeCloudException In case role type is 'Script' and it contains role
   *                                         members to be linked
   */
  public static void checkWhetherDynamicRoleIsNotHavingAnyMembers(final RolePojo roleRow)
      throws CyberArkPrivilegeCloudException {
    if (StringUtils.isNotBlank(roleRow.getRoleType()) && roleRow.getRoleType()
        .equalsIgnoreCase(RoleType.SCRIPT.getAttributeName()) && (!roleRow.getUsers().isEmpty()
        || !roleRow.getRoles().isEmpty())) {
      throw new CyberArkPrivilegeCloudException(
          "For Dynamic role (Script), members cannot be added manually");
    }
  }

  /**
   * This method will assign mapped role type based on provided role type.
   *
   * @param value   Provided role type
   * @param roleRow RolePojo where mapped value will be updated
   * @throws CyberArkPrivilegeCloudException In case mapped value not found
   */
  public static void checkAndAssignRoleType(final String value, final RolePojo roleRow)
      throws CyberArkPrivilegeCloudException {
    RoleType roleType =
        Arrays.stream(RoleType.values()).filter(x -> x.getAttributeName().equalsIgnoreCase(value))
            .findFirst().orElse(null);
    if (roleType == null) {
      throw new CyberArkPrivilegeCloudException(
          String.format("invalid role type provided:%s", value));
    }
    roleRow.setRoleType(roleType.getAttributeName());
  }

  /**
   * This method will update role description.
   *
   * @param attribute     Attribute containing description value
   * @param roleRecordRow Row object to be updated
   * @throws CyberArkPrivilegeCloudException In case values size is not one
   */
  public static void updateRoleObjectForDescription(final Attribute attribute,
                                                    final RecordRow roleRecordRow)
      throws CyberArkPrivilegeCloudException {
    if (attribute.getValues().size() != 1 || StringUtils.isBlank(attribute.getValues().get(0))) {
      throw new CyberArkPrivilegeCloudException(
          "only single non empty value is allowed for description attribute");
    }
    roleRecordRow.setDescription(attribute.getValues().get(0));
  }

  /**
   * This method will update role object for deleting all members.
   *
   * @param roleRecordRow Row that will contain values to be updated
   * @param members       Role members to be removed
   * @throws CyberArkPrivilegeCloudException In case role member is other than user/role
   */
  public static void updateRoleObjectForDeleteAllMembers(final RecordRow roleRecordRow,
                                                         final List<Record> members)
      throws CyberArkPrivilegeCloudException {
    Users users = new Users();
    Roles roles = new Roles();
    Groups groups = new Groups();
    for (Record member : members) {
      RecordRow recordRow = member.getRecordRow();
      if (recordRow.getMemberType().equalsIgnoreCase(RoleMemberType.USER.getMemberTypeValue())) {
        users.getKeysToDeleteAsRoleMembers().add(recordRow.getId());
      } else if (recordRow.getMemberType()
          .equalsIgnoreCase(RoleMemberType.ROLE.getMemberTypeValue())) {
        roles.getKeysToDeleteAsRoleMembers().add(recordRow.getId());
      } else if (recordRow.getMemberType()
          .equalsIgnoreCase(RoleMemberType.GROUP.getMemberTypeValue())) {
        groups.getKeysToDeleteAsRoleMembers().add(recordRow.getId());
      } else {
        throw new CyberArkPrivilegeCloudException(
            String.format("only user, role or group member type can be deleted, found: %s",
                recordRow.getMemberType()));
      }
    }
    roleRecordRow.setUsers(users);
    roleRecordRow.setRoles(roles);
    roleRecordRow.setGroups(groups);
  }

  /**
   * This method will prepare role map for each entry of fetched roles.
   *
   * @param role              Role object containing fetched role data
   * @param membersStringType List of role members in string format
   * @param fieldNames        Field Names as per schema
   * @return Map containing role data
   * @throws CyberArkPrivilegeCloudException In case invalid attribute name is received
   */
  public static Map<String, Object> getRoleMap(final Record role, final String membersStringType,
                                               final List<String> fieldNames)
      throws CyberArkPrivilegeCloudException {
    Map<String, Object> roleMap = new HashMap<>();
    for (String fieldName : fieldNames) {
      Role roleAttribute =
          Arrays.stream(Role.values()).filter(x -> x.getAttributeName().equalsIgnoreCase(fieldName))
              .findFirst().orElseThrow(IllegalStateException::new);
      populateRoleFields(role, membersStringType, fieldName, roleAttribute, roleMap);
    }
    return roleMap;
  }

  /**
   * This method will populate role fields.
   *
   * @param role              Role object containing etched role data
   * @param membersStringType role members in string format
   * @param fieldName         Field name to populate
   * @param roleAttribute     Role attribute corresponding to field name
   * @param roleMap           Role map that will store field value
   * @throws CyberArkPrivilegeCloudException In case invalid attribute name is received
   */
  public static void populateRoleFields(final Record role, final String membersStringType,
                                        final String fieldName, final Role roleAttribute,
                                        final Map<String, Object> roleMap)
      throws CyberArkPrivilegeCloudException {
    switch (roleAttribute) {
      case ID: {
        roleMap.put(Role.ID.getAttributeName(), role.getRecordRow().getId());
        break;
      }
      case NAME: {
        roleMap.put(Role.NAME.getAttributeName(), role.getRecordRow().getName());
        break;
      }
      case DESCRIPTION: {
        roleMap.put(Role.DESCRIPTION.getAttributeName(),
            StringUtils.isNotBlank(role.getRecordRow().getDescription()) ?
                role.getRecordRow().getDescription() : null);
        break;
      }
      case ROLE_TYPE: {
        roleMap.put(Role.ROLE_TYPE.getAttributeName(), role.getRecordRow().getRoleType());
        break;
      }
      case MEMBERS: {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        roleMap.put(Role.MEMBERS.getAttributeName(), StringUtils.isNotBlank(membersStringType) ?
            GOOGLE_JSON.fromJson(membersStringType, listType) : role.getRecordRow().getMembers());
        break;
      }
      case ROLE_RIGHTS: {
        roleMap.put(Role.ROLE_RIGHTS.getAttributeName(), role.getRoleRights());
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("invalid attribute name received:%s", fieldName));
      }
    }
  }

  /**
   * This method will fetch matching role attribute.
   *
   * @param attributeName Attribute name for which matching name is required
   * @return Matched name
   */
  public static Role getRoleAttributeForUpdate(final String attributeName) {
    return Arrays.stream(Role.values()).filter(
        x -> StringUtils.isNotBlank(attributeName) && x.getAttributeName()
            .equalsIgnoreCase(attributeName)).findFirst().orElseThrow(IllegalStateException::new);
  }

  /**
   * This method will parse member uuids to be linked as role members.
   *
   * @param value It can be plain uuid or uuids in a json array
   * @return Parsed uuids
   */
  @NonNull
  public static Set<String> extractUuids(final String value) {
    Type setType = new TypeToken<Set<String>>() {
    }.getType();
    Set<String> uuids = new HashSet<>();
    try {
      uuids = GOOGLE_JSON.fromJson(value, setType);
    } catch (JsonSyntaxException e) {
      uuids.add(value);
    }
    return uuids;
  }

  /**
   * This method will get matching role attribute name.
   *
   * @param attributeName Attribute name for which matching role attribute to be fetched
   * @return Matched Role attribute name
   */
  @Nullable
  public static Role getMatchingRoleAttribute(final String attributeName) {
    return Arrays.stream(Role.values()).filter(
        x -> StringUtils.isNotBlank(attributeName) && x.getAttributeName()
            .equalsIgnoreCase(attributeName)).findFirst().orElse(null);
  }

  /**
   * Method to attach role rights to a role.
   *
   * @param role             Role to whom rights to be attached
   * @param cyberArkResponse Response containing role rights to be attached
   * @throws CyberArkPrivilegeCloudException In case role object or response for role rights is
   *                                         null
   */
  public static void attachRoleRightsToRole(final Record role,
                                            final CyberArkResponse cyberArkResponse)
      throws CyberArkPrivilegeCloudException {
    if (role == null || cyberArkResponse == null || cyberArkResponse.getResultData() == null
        || cyberArkResponse.getResultData().getRecords() == null) {
      throw new CyberArkPrivilegeCloudException(
          "either role or role rights api response is null");
    }
    List<RoleRight> roleRights =
        cyberArkResponse.getResultData().getRecords().stream()
            .filter(record -> record != null && record.getRecordRow() != null)
            .map(Record::getRecordRow)
            .map(row -> {
              RoleRight roleRight = new RoleRight();
              roleRight.setPath(row.getPath());
              roleRight.setScope(row.getScope());
              roleRight.setDescription(row.getDescription());
              roleRight.setServiceName(row.getServiceName());
              return roleRight;
            })
            .collect(Collectors.toList());
    if (log.isDebugEnabled()) {
      log.debug("role rights fetched for role:{} are:{}", role.getRecordRow().getName(),
          roleRights.size());
    }
    role.setRoleRights(roleRights);
  }

  /**
   * Method to populate filter map for roles.
   *
   * @param filter          Search filter containing filter received.
   * @param matchingFilters Map which has to be populated.
   */
  public static void populateFilterMapForRole(final SearchFilter filter,
                                              final Map<String, List<String>> matchingFilters) {
    String attributeName = filter.getAttribute();
    String attributeValue = filter.getAssertionValue().orElse(null);
    SearchFilter.Type searchFilterType = filter.getType();
    if (StringUtils.isBlank(attributeValue) || NULL_KEY.equalsIgnoreCase(attributeValue.trim())) {
      log.info("either attribute value is blank or having value 'null'");
      return;
    }
    Role matchingAttribute = RoleUtils.getMatchingRoleAttribute(attributeName);
    if (matchingAttribute != null && !NOT_OPERATOR_KEY.equalsIgnoreCase(searchFilterType.name())) {
      switch (matchingAttribute) {
        case ID:
        case NAME:
        case ROLE_TYPE:
        case DESCRIPTION: {
          matchingFilters.computeIfAbsent(
                  matchingAttribute.getAttributeName().toLowerCase(Locale.ROOT),
                  values -> new ArrayList<>())
              .add(attributeValue.toLowerCase(Locale.ROOT));
          break;
        }
      }
    } else {
      log.info("no matching filter attribute found/NOT operator not allowed");
    }
  }

  /**
   * Method to check whether role rights to be fetched.
   *
   * @param schema           Schema object
   * @param schemaObjectName Schema object name
   * @return boolean whether role rights to be fetched
   */
  public static boolean checkWhetherFetchRoleRights(final Schema schema,
                                                    final String schemaObjectName) {
    List<String> fieldNames = Utils.getFieldNames(
        Utils.getSchemaObject(schema, schemaObjectName));
    return fieldNames.stream()
        .map(String::toLowerCase)
        .anyMatch(Role.ROLE_RIGHTS.getAttributeName().toLowerCase(Locale.ROOT)::equals);
  }

  /**
   * Method to get roles payload json string when filter is there.
   *
   * @param pageNumber Page number
   * @param pageSize   Page Size
   * @param filters    Filter to be applied
   * @return Payload json string
   */
  @NotNull
  public static String getPayloadWithFilters(int pageNumber, int pageSize,
                                             final Map<String, String> filters) {
    String keyName = filters.keySet().iterator().next();
    String value = filters.get(keyName);
    boolean useLike = value.contains(ASTERISK_KEY);
    if (useLike) {
      value = value.replace(ASTERISK_KEY, PERCENTAGE_KEY);
    }
    String template = useLike
        ? GET_ROLES_PAYLOAD_FOR_FILTERS_WITH_LIKE_COMMAND
        : GET_ROLES_PAYLOAD_FOR_FILTERS_WITH_EXACT_VALUE;
    return String.format(
        template,
        keyName,
        value,
        pageNumber,
        pageSize,
        pageSize
    );
  }
}