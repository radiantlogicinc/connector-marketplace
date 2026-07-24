package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE;
import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.GROUP_START_INDEX;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.START_INDEX;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.enums.Role;
import com.radiantlogic.enums.RoleMemberType;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.Modification;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Groups;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.RoleUtils;
import com.radiantlogic.utility.Roles;
import com.radiantlogic.utility.Users;
import com.radiantlogic.utility.Utils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will handle update object related operations.
 */
@Slf4j
@SuppressWarnings(PMD_KEY)
public class UpdateRoleRequest {

  /**
   * This object will be used to get data for various objects.
   */
  private final GetRequest getRequest;
  /**
   * This object will be used to get url & payload for various create objects operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;

  /**
   * This constructor will initialize class's instance members.
   *
   * @param getRequest      GetRequest class object
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   */
  public UpdateRoleRequest(final GetRequest getRequest, final RequestBuilder requestBuilder,
                           final RequestExecutor requestExecutor) {
    this.getRequest = getRequest;
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will process role update request.
   *
   * @param roleName          Role name of role to be updated
   * @param ldapModifyRequest update request details
   * @param schemaObject      Request schema object
   * @throws CyberArkPrivilegeCloudException In case of invalid attribute for update/response not
   *                                         ok
   * @throws IOException                     In case of connectivity error
   */
  public void processRoleUpdate(final String roleName, final LdapModifyRequest ldapModifyRequest,
                                final SchemaObject schemaObject)
      throws CyberArkPrivilegeCloudException, IOException {
    List<Record> roles = getRequest.getRoles(0, roleName, false, null, null, false, null);
    if (roles.isEmpty()) {
      throw new CyberArkPrivilegeCloudException(
          String.format("no matching role found for role name:%s", roleName));
    }
    RecordRow roleRecordRow = new RecordRow();
    roleRecordRow.setName(roles.get(0).getRecordRow().getId());
    for (Modification modification : ldapModifyRequest.getModifications()) {
      Attribute attribute = modification.getAttribute();
      Modification.ModificationType modificationType = modification.getModificationType();
      log.debug(ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE, attribute.getName(),
          attribute.getValues(), modificationType.name());
      String attributeName = Utils.getAttributeName(schemaObject, attribute.getName());
      Role attributeForUpdate = RoleUtils.getRoleAttributeForUpdate(attributeName);
      processRoleUpdateBasedOnModificationType(modificationType, attribute, roleRecordRow,
          attributeForUpdate);
    }
    updateRole(roleRecordRow);
  }

  private void updateRoleObjectForDeleteMembersAttribute(final RecordRow roleRecordRow)
      throws CyberArkPrivilegeCloudException, IOException {
    List<Record> members = getRequest.getRoleMembers(roleRecordRow.getName());
    if (!members.isEmpty()) {
      RoleUtils.updateRoleObjectForDeleteAllMembers(roleRecordRow, members);
    }
  }

  private void updateRoleObjectForDeleteMember(final Attribute attribute,
                                               final RecordRow roleRecordRow)
      throws CyberArkPrivilegeCloudException, IOException {
    String memberPrefixedWithType = attribute.getValues().get(0);
    if (memberPrefixedWithType.contains(RoleMemberType.USER.getMemberTypeKey())) {
      String userName = memberPrefixedWithType.substring(START_INDEX);
      List<Record> usersList = getRequest.getUsers(0, userName, null, null, null);
      if (usersList.isEmpty()) {
        throw new CyberArkPrivilegeCloudException(String.format("username not found:%s", userName));
      }
      Users users = new Users();
      users.setKeysToDeleteAsRoleMembers(
          Collections.singleton(usersList.get(0).getRecordRow().getId()));
      roleRecordRow.setUsers(users);
    } else if (memberPrefixedWithType.contains(RoleMemberType.ROLE.getMemberTypeKey())) {
      String roleName = memberPrefixedWithType.substring(START_INDEX);
      List<Record> rolesList = getRequest.getRoles(0, roleName, false, null, null, false, null);
      if (rolesList.isEmpty()) {
        throw new CyberArkPrivilegeCloudException(
            String.format("role name not found:%s", roleName));
      }
      Roles roles = new Roles();
      roles.setKeysToDeleteAsRoleMembers(
          Collections.singleton(rolesList.get(0).getRecordRow().getId()));
      roleRecordRow.setRoles(roles);
    } else if (memberPrefixedWithType.contains(RoleMemberType.GROUP.getMemberTypeKey())) {
      String groupName = memberPrefixedWithType.substring(GROUP_START_INDEX);
      Groups groups = new Groups();
      groups.setKeysToDeleteAsRoleMembers(Collections.singleton(groupName));
      roleRecordRow.setGroups(groups);
    } else {
      throw new CyberArkPrivilegeCloudException(
          String.format("invalid member type value received:%s", memberPrefixedWithType));
    }
  }

  private void updateRoleObjectForDeleteOperation(final Attribute attribute,
                                                  final RecordRow roleRecordRow,
                                                  final Role attributeForUpdate)
      throws CyberArkPrivilegeCloudException, IOException {
    if (attributeForUpdate.getAttributeName()
        .equalsIgnoreCase(Role.DESCRIPTION.getAttributeName())) {
      roleRecordRow.setDescription(StringUtils.EMPTY);
    } else if (attributeForUpdate.getAttributeName()
        .equalsIgnoreCase(Role.MEMBERS.getAttributeName())) {
      // case of delete members attribute
      if (attribute.getValues().isEmpty()) {
        updateRoleObjectForDeleteMembersAttribute(roleRecordRow);
      } else { // case of delete single member
        updateRoleObjectForDeleteMember(attribute, roleRecordRow);
      }
    } else {
      throw new CyberArkPrivilegeCloudException(
          String.format("invalid attribute name for delete operation:%s", attribute.getName()));
    }
  }

  private void updateRoleObjectForAddOperation(final Attribute attribute,
                                               final RecordRow roleRecordRow,
                                               final Role attributeForUpdate)
      throws CyberArkPrivilegeCloudException, IOException {
    if (attributeForUpdate.getAttributeName()
        .equalsIgnoreCase(Role.DESCRIPTION.getAttributeName())) {
      RoleUtils.updateRoleObjectForDescription(attribute, roleRecordRow);
    } else if (attributeForUpdate.getAttributeName()
        .equalsIgnoreCase(Role.MEMBERS.getAttributeName())) {
      Set<String> allUuids = RoleUtils.getMemberUuids(attribute);
      if (allUuids.contains(roleRecordRow.getName().toLowerCase(Locale.ROOT))) {
        throw new CyberArkPrivilegeCloudException("role itself cannot be added as a member");
      }
      verifyUuids(allUuids, roleRecordRow);
    } else {
      throw new CyberArkPrivilegeCloudException(
          String.format("invalid attribute name for add:%s", attribute.getName()));
    }
  }

  @SuppressWarnings(PMD_KEY)
  private void updateRoleObjectForReplaceOperation(final Attribute attribute,
                                                   final RecordRow roleRecordRow,
                                                   final Role attributeForUpdate)
      throws CyberArkPrivilegeCloudException, IOException {
    if (attributeForUpdate.getAttributeName()
        .equalsIgnoreCase(Role.DESCRIPTION.getAttributeName())) {
      RoleUtils.updateRoleObjectForDescription(attribute, roleRecordRow);
    } else if (attributeForUpdate.getAttributeName()
        .equalsIgnoreCase(Role.MEMBERS.getAttributeName())) {
      List<String> attributeValues = attribute.getValues();
      if (attributeValues.isEmpty()) {
        throw new CyberArkPrivilegeCloudException("attribute values can't be blank");
      }
      String value = attributeValues.get(attributeValues.size() - 1);
      if (StringUtils.isBlank(value)) {
        throw new CyberArkPrivilegeCloudException("received value is blank");
      }
      Set<String> uuids = RoleUtils.extractUuids(value).stream().map(String::toLowerCase)
          .collect(Collectors.toSet());
      if (uuids.contains(roleRecordRow.getName().toLowerCase(Locale.ROOT))) {
        throw new CyberArkPrivilegeCloudException("role itself cannot be added as a member");
      }
      verifyUuids(uuids, roleRecordRow);
    } else {
      throw new CyberArkPrivilegeCloudException(
          String.format("invalid attribute name for replace:%s", attribute.getName()));
    }
  }

  private void processRoleUpdateBasedOnModificationType(
      final Modification.ModificationType modificationType, final Attribute attribute,
      final RecordRow roleRecordRow, final Role attributeForUpdate)
      throws CyberArkPrivilegeCloudException, IOException {
    switch (modificationType) {
      case ADD: {
        updateRoleObjectForAddOperation(attribute, roleRecordRow, attributeForUpdate);
        break;
      }
      case REPLACE: {
        updateRoleObjectForReplaceOperation(attribute, roleRecordRow, attributeForUpdate);
        break;
      }
      case DELETE: {
        updateRoleObjectForDeleteOperation(attribute, roleRecordRow, attributeForUpdate);
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("invalid modification type received: %s", modificationType.name()));
      }
    }
  }

  /**
   * This method will perform update role operation.
   *
   * @param roleRecordRow It will contain role details
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  protected void updateRole(final RecordRow roleRecordRow)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getUpdateRoleUrl())
        .post(requestBuilder.getUpdateRolePayload(roleRecordRow))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("update role api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        log.debug("role with id:{} successfully updated", roleRecordRow.getName());
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("update role api response message is: %s", apiResponse));
  }

  /**
   * This method will verify uuids (to be linked role members uuids).
   *
   * @param allUuids      To be linked role members uuids
   * @param roleRecordRow Row where uuids will be set
   * @throws CyberArkPrivilegeCloudException In case invalid uuid is passed
   * @throws IOException                     In case of api connectivity error
   */
  public void verifyUuids(final Set<String> allUuids, final RecordRow roleRecordRow)
      throws CyberArkPrivilegeCloudException, IOException {
    Set<String> fetchedUserIds =
        getRequest.getUsers(0, null, null, allUuids, null).stream()
            .map(x -> x.getRecordRow().getId())
            .collect(Collectors.toSet());
    Set<String> matchingUserUuids =
        allUuids.stream().filter(fetchedUserIds::contains).collect(Collectors.toSet());
    if (!matchingUserUuids.isEmpty()) {
      Users users = new Users();
      users.getKeysToAddAsRoleMembers().addAll(matchingUserUuids);
      roleRecordRow.setUsers(users);
    }
    Set<String> nonMatchingList =
        allUuids.stream().filter(s -> !fetchedUserIds.contains(s)).collect(Collectors.toSet());
    if (!nonMatchingList.isEmpty()) {
      Set<String> fetchedRoleIds =
          getRequest.getRoles(0, null, false, null, nonMatchingList, false, null).stream()
              .map(x -> x.getRecordRow().getId()).collect(Collectors.toSet());
      Set<String> matchingRoleUuids =
          nonMatchingList.stream().filter(fetchedRoleIds::contains).collect(Collectors.toSet());
      Set<String> finalNonMatchingList =
          nonMatchingList.stream().filter(s -> !fetchedRoleIds.contains(s))
              .collect(Collectors.toSet());
      if (!finalNonMatchingList.isEmpty()) {
        throw new CyberArkPrivilegeCloudException("one or more uuids is invalid/doesn't exist");
      }
      Roles roles = new Roles();
      roles.getKeysToAddAsRoleMembers().addAll(matchingRoleUuids);
      roleRecordRow.setRoles(roles);
    }
  }

}
