package com.radiantlogic.create;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.enums.User;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CreateObjectResult;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.UserUtils;
import com.radiantlogic.utility.Utils;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/**
 * This class will contain functionality for creating objects.
 */
@Slf4j
public class CreateUserRequest {

  /**
   * This object will be used to get url & payload for various create objects operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;

  /**
   * This constructor will be used to initialize CreateRequest class's instance members.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   */
  public CreateUserRequest(final RequestBuilder requestBuilder,
                           final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will process create user request.
   *
   * @param ldapAddRequest LdapAddRequest containing create user details
   * @param schemaObject   Request schema
   * @param uniqueKey      Username of user to be created
   * @throws CyberArkPrivilegeCloudException In case response is not ok
   * @throws IOException                     In case of connectivity error
   */
  public void processCreateUserRequest(final LdapAddRequest ldapAddRequest,
                                       final SchemaObject schemaObject, final String uniqueKey)
      throws CyberArkPrivilegeCloudException, IOException {
    RecordRow userRecordRow = new RecordRow();
    for (Attribute attribute : ldapAddRequest.getAttributes()) {
      log.debug("attribute name is:{} with values:{}", attribute.getName(), attribute.getValues());
      String attributeName = Utils.getAttributeName(schemaObject, attribute.getName());
      User attributeReceivedForCreate = UserUtils.getMatchingUserAttribute(attributeName);
      if (attributeReceivedForCreate != null && Utils.isAttributeValuesSizeIsOne(attribute)) {
        UserUtils.populateCreateUserObject(attributeReceivedForCreate, uniqueKey, attribute,
            userRecordRow);
      }
    }
    UserUtils.setAdditionalCreateUserAttributes(userRecordRow);
    createUser(userRecordRow);
  }

  /**
   * This method will perform create user operation.
   *
   * @param userRecordRow It will contain user details
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  protected void createUser(final RecordRow userRecordRow)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getCreateUserUrl())
        .post(requestBuilder.getCreateUserPayload(userRecordRow))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("create user api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CreateObjectResult result = GOOGLE_JSON.fromJson(apiResponse, CreateObjectResult.class);
      if (result.isSuccess()) {
        log.debug("user created with id:{}", result.getId());
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("create user api response message is: %s", apiResponse));
  }
}
