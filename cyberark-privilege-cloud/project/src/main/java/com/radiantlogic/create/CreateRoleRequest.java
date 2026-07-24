package com.radiantlogic.create;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.enums.Role;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.RolePojo;
import com.radiantlogic.utility.RoleUtils;
import com.radiantlogic.utility.Utils;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/**
 * This class will contain functionality for creating objects.
 */
@Slf4j
public class CreateRoleRequest {

  /**
   * This object will be used to get url & payload for various create objects operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * This object will be used to get data for various objects.
   */
  private final GetRequest getRequest;
  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;

  /**
   * This constructor will be used to initialize CreateRequest class's instance members.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param getRequest      GetRequest class object
   * @param requestExecutor Executor class object
   */
  public CreateRoleRequest(final RequestBuilder requestBuilder, final GetRequest getRequest,
                           final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.getRequest = getRequest;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will process create role request.
   *
   * @param ldapAddRequest Request details
   * @param schemaObject   Request schema object
   * @param uniqueKey      Role name to be created
   * @throws CyberArkPrivilegeCloudException In case response is not ok
   * @throws IOException                     In case of connectivity error
   */
  public void processCreateRoleRequest(final LdapAddRequest ldapAddRequest,
                                       final SchemaObject schemaObject, final String uniqueKey)
      throws CyberArkPrivilegeCloudException, IOException {
    RolePojo roleRow = new RolePojo();
    for (Attribute attribute : ldapAddRequest.getAttributes()) {
      log.debug("attribute name is:{} with values:{}", attribute.getName(), attribute.getValues());
      String attributeName = Utils.getAttributeName(schemaObject, attribute.getName());
      Role attributeReceivedForCreate = RoleUtils.getMatchingRoleAttribute(attributeName);
      if (attributeReceivedForCreate != null) {
        populateCreateRoleObject(attributeReceivedForCreate, attribute, uniqueKey, roleRow);
      }
    }
    RoleUtils.checkWhetherDynamicRoleIsNotHavingAnyMembers(roleRow);
    createRole(roleRow);
  }

  private void populateCreateRoleObject(final Role attributeReceivedForCreate,
                                        final Attribute attribute, final String uniqueKey,
                                        final RolePojo rolePojo)
      throws CyberArkPrivilegeCloudException, IOException {
    String value = attribute.getValues().get(0);
    switch (attributeReceivedForCreate) {
      case NAME:
      case ROLE: {
        Utils.isAttributeValuesSizeIsOne(attribute);
        if (!uniqueKey.equals(value)) {
          throw new CyberArkPrivilegeCloudException(
              "Name and rdn value has to be same for role creation");
        }
        rolePojo.setName(value);
        break;
      }
      case DESCRIPTION: {
        Utils.isAttributeValuesSizeIsOne(attribute);
        rolePojo.setDescription(value);
        break;
      }
      case ROLE_TYPE: {
        Utils.isAttributeValuesSizeIsOne(attribute);
        RoleUtils.checkAndAssignRoleType(value, rolePojo);
        break;
      }
      case MEMBERS: {
        verifyUuids(RoleUtils.getMemberUuids(attribute), rolePojo);
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format("invalid attribute received for role creation:%s", attribute.getName()));
      }
    }
  }

  /**
   * This method will verify uuids (to be linked role members uuids).
   *
   * @param allUuids To be linked role members uuids
   * @param rolePojo RolePojo where uuids will be set
   * @throws CyberArkPrivilegeCloudException In case invalid uuid is passed
   * @throws IOException                     In case of api connectivity error
   */
  public void verifyUuids(final Set<String> allUuids, final RolePojo rolePojo)
      throws CyberArkPrivilegeCloudException, IOException {
    Set<String> fetchedUserIds =
        getRequest.getUsers(0, null, null, allUuids, null).stream()
            .map(x -> x.getRecordRow().getId())
            .collect(Collectors.toSet());
    Set<String> matchingUserUuids =
        allUuids.stream().filter(fetchedUserIds::contains).collect(Collectors.toSet());
    if (!matchingUserUuids.isEmpty()) {
      rolePojo.setUsers(matchingUserUuids);
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
      rolePojo.setRoles(matchingRoleUuids);
    }
  }

  /**
   * This method will perform create role operation.
   *
   * @param rolePojo It will contain role details
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  protected void createRole(final RolePojo rolePojo)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getCreateRoleUrl())
        .post(requestBuilder.getCreateRolePayload(rolePojo))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("create role api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        log.debug("role created with id:{}", cyberArkResponse.getResultData().getId());
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("create role api response message is: %s", apiResponse));
  }
}
