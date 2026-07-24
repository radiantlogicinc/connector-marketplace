package com.radiantlogic.delete;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.SYSTEM_ADMINISTRATOR_ROLE_NAME_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/**
 * This class will handle delete object's operations.
 */
@Slf4j
public class DeleteRoleRequest {

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
   * This constructor will initialize instance members of the class.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param getRequest      GetRequest class object
   * @param requestExecutor Executor class object
   */
  public DeleteRoleRequest(final RequestBuilder requestBuilder, final GetRequest getRequest,
                           final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.getRequest = getRequest;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will fetch and delete role.
   *
   * @param roleName Role name to be deleted
   * @return LdapResponse Whether success or failure
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   * @throws IOException                     In case of connectivity error
   */
  public LdapResponse<String> fetchAndDeleteRole(final String roleName)
      throws CyberArkPrivilegeCloudException, IOException {
    if (SYSTEM_ADMINISTRATOR_ROLE_NAME_KEY.equalsIgnoreCase(roleName)) {
      log.error("can't delete role required to run the connector:{}", roleName);
      return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
    }
    List<Record> roles = getRequest.getRoles(0, roleName, false, null, null, false, null);
    if (!roles.isEmpty()) {
      String roleId = roles.get(0).getRecordRow().getId();
      deleteRole(roleId, roleName);
      return new LdapResponse<>(LdapResultCode.SUCCESS);
    }
    log.error("no role found with name:{}", roleName);
    return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
  }

  /**
   * This method will perform delete role operation.
   *
   * @param roleId   Role id for role to be deleted
   * @param roleName Role name for role to be deleted
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  protected void deleteRole(final String roleId, final String roleName)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getDeleteRoleUrl())
        .post(requestBuilder.getDeleteRolePayload(roleId))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get delete role api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        log.debug("role with name:{} deleted", roleName);
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("delete role api response message is: %s", apiResponse));

  }
}
