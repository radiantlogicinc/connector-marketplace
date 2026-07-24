package com.radiantlogic.delete;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
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
public class DeleteUserRequest {

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
  public DeleteUserRequest(final RequestBuilder requestBuilder, final GetRequest getRequest,
                           final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.getRequest = getRequest;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will fetch and delete user.
   *
   * @param userName Username to be deleted
   * @return LdapResponse Whether success or failure
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   * @throws IOException                     In case of connectivity error
   */
  public LdapResponse<String> fetchAndDeleteUser(final String userName)
      throws CyberArkPrivilegeCloudException, IOException {
    if (userName.equalsIgnoreCase(requestBuilder.getClientId())) {
      log.error("can't delete user with which the connector is configured:{}", userName);
      return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
    }
    List<Record> users = getRequest.getUsers(0, userName, null, null, null);
    if (!users.isEmpty()) {
      String userId = users.get(0).getRecordRow().getId();
      deleteUser(userId, userName);
      return new LdapResponse<>(LdapResultCode.SUCCESS);
    }
    log.error("no user found with key:{}", userName);
    return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
  }

  /**
   * This operation will perform delete user operation.
   *
   * @param userId   User id of user to be deleted
   * @param userName Username of user to be deleted
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  protected void deleteUser(final String userId, final String userName)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getDeleteUserUrl())
        .post(requestBuilder.getDeleteUserPayload(userId))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get delete user api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        log.debug("user with username:{} deleted", userName);
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("delete user api response message is: %s", apiResponse));
  }
}
