package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.UserUtils;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/**
 * This class will handle update object related operations.
 */
@Slf4j
public class UpdateUserRequest {

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
  public UpdateUserRequest(final GetRequest getRequest, final RequestBuilder requestBuilder,
                           final RequestExecutor requestExecutor) {
    this.getRequest = getRequest;
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will process user update request.
   *
   * @param username          Username of user to be updated
   * @param ldapModifyRequest Update details
   * @param schemaObject      Request schema object
   * @throws CyberArkPrivilegeCloudException In case of invalid attribute for update/response not
   *                                         ok
   * @throws IOException                     In case of connectivity error
   */
  public void processUserUpdate(final String username, final LdapModifyRequest ldapModifyRequest,
                                final SchemaObject schemaObject)
      throws CyberArkPrivilegeCloudException, IOException {
    List<Record> users = getRequest.getUsers(0, username, null, null, null);
    if (users.isEmpty()) {
      throw new CyberArkPrivilegeCloudException(
          String.format("no matching user found for username:%s", username));
    }
    RecordRow userRecordRow =
        UserUtils.populateUserObjectForUpdate(ldapModifyRequest, schemaObject, users);
    updateUser(userRecordRow);
  }

  /**
   * This method will perform update user operation.
   *
   * @param userRecordRow It will contain user details
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  protected void updateUser(final RecordRow userRecordRow)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getUpdateUserUrl())
        .post(requestBuilder.getUpdateUserPayload(userRecordRow))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("update user api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        log.debug("user with id:{} successfully updated", userRecordRow.getId());
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("update user api response message is: %s", apiResponse));
  }
}
