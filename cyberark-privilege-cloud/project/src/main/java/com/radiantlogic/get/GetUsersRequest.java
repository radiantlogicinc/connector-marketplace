package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.ResponseData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/**
 * This class will get data for various objects.
 */
@Slf4j
public class GetUsersRequest {

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
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   */
  public GetUsersRequest(final RequestBuilder requestBuilder,
                         final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will fetch specific page's users as part of paginated response or a specific user.
   *
   * @param pageNumber Page for which users to be fetched
   * @param username   Username for which details to be fetched
   * @param userId     User id for which details to be fetched
   * @param userIds    User ids for which details to be fetched
   * @param filters    Filters to be applied while getting users
   * @return List of users fetched
   * @throws IOException                     In case of making http call or processing response
   * @throws CyberArkPrivilegeCloudException In case api response is not OK
   */
  public List<Record> getUsers(final int pageNumber, final String username, final String userId,
                               final Set<String> userIds, final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getRedRockUrl())
        .post(requestBuilder.getUsersPayload(pageNumber, username, userId, userIds, filters))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get user(s) api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        List<Record> users = new ArrayList<>(cyberArkResponse.getResultData().getRecords());
        log.debug("total users fetched are: {}", users.size());
        return users;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get users api response message is: %s", apiResponse));
  }
}
