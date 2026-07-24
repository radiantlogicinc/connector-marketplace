package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.SafeMember;
import com.radiantlogic.utility.SafeMembers;
import com.radiantlogic.utility.SafeRow;
import com.radiantlogic.utility.SafeUtils;
import com.radiantlogic.utility.SafesResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will get safes related data.
 */
@Slf4j
public class GetSafesRequest {

  /**
   * This object will be used to get url & payload for various objects operations.
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
  public GetSafesRequest(final RequestBuilder requestBuilder,
                         final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will get all safes/safe by name.
   *
   * @param safeName If a safe name is provided, then details for that safe will be returned,
   *                 otherwise all safes will be returned
   * @param nextLink Next link for pagination
   * @param filters  Filters to be applied while getting safes
   * @return SafesResult containing fetched safe(s) & an optional next link
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case response is not ok
   */
  public SafesResult getSafes(final String safeName, final String nextLink,
                              final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    HttpUrl safesUrl = requestBuilder.getSafesUrl(safeName, nextLink, filters);
    Request request = new Request.Builder().url(safesUrl).get()
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get safe(s) api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      if (StringUtils.isBlank(safeName)) {
        CyberArkResponse cyberArkResponse =
            GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
        List<SafeRow> safes = cyberArkResponse.getSafes();
        addAdditionalSafeAttributes(safes);
        SafesResult safesResult = new SafesResult();
        safesResult.setSafes(safes);
        safesResult.setNextLink(cyberArkResponse.getNextLink());
        return safesResult;
      }
      List<SafeRow> safes =
          Collections.singletonList(GOOGLE_JSON.fromJson(apiResponse, SafeRow.class));
      addAdditionalSafeAttributes(safes);
      SafesResult safesResult = new SafesResult();
      safesResult.setSafes(safes);
      return safesResult;
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get safes api response message is: %s", apiResponse));
  }

  private void addAdditionalSafeAttributes(final List<SafeRow> safes)
      throws CyberArkPrivilegeCloudException, IOException {
    SafeUtils.formatSafesDates(safes);
    for (SafeRow safe : safes) {
      List<SafeMember> members = getSafeMembers(safe.getSafeName());
      SafeUtils.formatMembersMembershipExpirationDate(members);
      safe.setMembers(members);
    }
  }

  @SuppressWarnings(PMD_KEY)
  private List<SafeMember> getSafeMembers(final String safeName)
      throws CyberArkPrivilegeCloudException, IOException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    String nextLink = "";
    List<SafeMember> allMembers = new ArrayList<>();
    do {
      HttpUrl safeMembersUrl = requestBuilder.getSafeMembersUrl(safeName, nextLink);
      Request request = new Request.Builder().url(safeMembersUrl).get()
          .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
      ResponseData responseData = requestExecutor.execute(request);
      int responseCode = responseData.getResponseCode();
      log.debug("get safe members api response code with message is: {} {}", responseCode,
          responseData.getResponseMessage());
      String apiResponse = responseData.getApiResponse();
      if (responseCode == HttpStatus.OK.asInt()) {
        SafeMembers result = GOOGLE_JSON.fromJson(apiResponse, SafeMembers.class);
        nextLink = result.getNextLink();
        List<SafeMember> members = result.getMembers();
        allMembers.addAll(members);
        log.debug("safe members fetched:{}", members.size());
      } else if (responseCode == HttpStatus.TOKEN_EXPIRED.asInt()) {
        requestExecutor.generateToken();
      } else {
        throw new CyberArkPrivilegeCloudException(
            String.format("get safe members api response is: %s", apiResponse));
      }
    } while (StringUtils.isNotBlank(nextLink));
    log.debug("total safe members fetched:{}", allMembers.size());
    return allMembers;
  }

}