package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.DirectoryServiceRecord;
import com.radiantlogic.utility.DirectoryServiceResponse;
import com.radiantlogic.utility.DirectoryServiceUtils;
import com.radiantlogic.utility.ResponseData;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will get directory services related data.
 */
@Slf4j
public class GetDirectoryServicesRequest {

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
  public GetDirectoryServicesRequest(final RequestBuilder requestBuilder,
                                     final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will return directory service(s).
   *
   * @param directoryServiceName If passed, will return matching directory service, else will return
   *                             all
   * @return List of directory services fetched
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  public List<DirectoryServiceRecord> getDirectoryServices(final String directoryServiceName)
      throws IOException, CyberArkPrivilegeCloudException {

    /*
      Design Rationale

      As there was no specific endpoint to fetch a single directory service, so get all directory
      services endpoint (non paginated) will be called even when request is to fetch a single
      directory service and then matching record will be filtered out based on name provided.
    */

    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    HttpUrl directoryServicesUrl = requestBuilder.getDirectoryServicesUrl();
    Request request = new Request.Builder().url(directoryServicesUrl)
        .post(requestBuilder.getDirectoryServicesRequestBody())
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get directory service(s) api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      DirectoryServiceResponse directoryServiceResponse =
          GOOGLE_JSON.fromJson(apiResponse, DirectoryServiceResponse.class);
      if (directoryServiceResponse.isSuccess()) {
        DirectoryServiceUtils.checkWhetherDirectoryServiceResponseIsNotNull(
            directoryServiceResponse);
        List<DirectoryServiceRecord> directoryServiceRecords =
            directoryServiceResponse.getResult().getResults();
        return StringUtils.isNotBlank(directoryServiceName)
            ? getDirectoryServiceRecord(directoryServiceName, directoryServiceRecords) :
            directoryServiceRecords;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get directory service(s) api response message is: %s", apiResponse));
  }

  private @NonNull List<DirectoryServiceRecord> getDirectoryServiceRecord(
      final String directoryServiceName,
      final List<DirectoryServiceRecord> directoryServiceRecords) {
    return
        directoryServiceRecords.stream()
            .filter(record ->
                record != null && record.getRow() != null && record.getRow().getName() != null
                    && record.getRow().getName().equalsIgnoreCase(directoryServiceName))
            .findFirst()
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());
  }
}