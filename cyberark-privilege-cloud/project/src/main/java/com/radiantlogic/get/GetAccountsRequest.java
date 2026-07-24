package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.AccountRow;
import com.radiantlogic.utility.AccountUtils;
import com.radiantlogic.utility.Accounts;
import com.radiantlogic.utility.ResponseData;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will get accounts related data.
 */
@Slf4j
public class GetAccountsRequest {

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
  public GetAccountsRequest(final RequestBuilder requestBuilder,
                            final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will get all accounts/account by id.
   *
   * @param accountId If an account id is provided, then details for that account will be returned,
   *                  otherwise all accounts will be returned
   * @param nextLink  Next link for pagination
   * @param filters   Filters to be applied while getting accounts
   * @return Accounts Containing fetched accounts & next link
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case response is not ok
   */
  public Accounts getAccounts(final String accountId, final String nextLink,
                              final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    HttpUrl accountsUrl = requestBuilder.getAccountsUrl(accountId, nextLink, filters);
    Request request = new Request.Builder().url(accountsUrl).get()
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get account(s) api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      if (StringUtils.isBlank(accountId)) {
        Accounts accountsResponse = GOOGLE_JSON.fromJson(apiResponse, Accounts.class);
        List<AccountRow> accounts = accountsResponse.getAccountsList();
        AccountUtils.formatAccountsDates(accounts);
        return accountsResponse;
      }
      List<AccountRow> accounts =
          Collections.singletonList(GOOGLE_JSON.fromJson(apiResponse, AccountRow.class));
      if (accounts.get(0).getDeletionTime() != null) {
        throw new CyberArkPrivilegeCloudException(
            String.format("account with id: %s has been deleted.", accountId));
      }
      AccountUtils.formatAccountsDates(accounts);
      Accounts accountsResponse = new Accounts();
      accountsResponse.setAccountsList(accounts);
      return accountsResponse;
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get accounts api response message is: %s", apiResponse));
  }
}