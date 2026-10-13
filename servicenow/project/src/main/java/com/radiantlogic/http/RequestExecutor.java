package com.radiantlogic.http;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.model.ResponseData;
import java.io.IOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to execute a request.
 */
@Slf4j
@Getter
public class RequestExecutor {

  /**
   * Field to store http client.
   */
  private final HttpClient httpClient;

  /**
   * Constructor to initialize instance members.
   *
   * @param httpClient Httpclient instance.
   */
  public RequestExecutor(final HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Method to execute a request.
   *
   * @param request Request object containing request details
   * @return ResponseData Containing Api response data
   * @throws IOException         In case of connectivity error
   * @throws ServiceNowException In case api response is null
   */
  public ResponseData execute(final Request request)
      throws IOException, ServiceNowException {
    try (Response response = httpClient.getOkHttpClient().newCall(request).execute();
         ResponseBody body = response.body()) {
      int responseCode = response.code();
      String responseMessage = response.message();
      log.debug("api response code is: {} with message: {}", responseCode, responseMessage);
      if (body != null) {
        return new ResponseData(body.string(), responseMessage, responseCode, response.headers());
      }
      throw new ServiceNowException(
          String.format("api response message is: %s", responseMessage));
    }
  }

  /**
   * Method to execute delete object request. It will not have response body for success (204).
   *
   * @param request Delete object request
   * @throws IOException         In case of connectivity error
   * @throws ServiceNowException In case api response is not ok
   */
  public void executeDelete(final Request request)
      throws IOException, ServiceNowException {
    try (Response response =
             httpClient.getOkHttpClient().newCall(request).execute();
         ResponseBody body = response.body()) {
      int responseCode = response.code();
      String responseMessage = response.message();
      log.debug("api response code is: {} with message: {}", responseCode, responseMessage);
      if (responseCode == HttpStatus.NO_CONTENT.asInt()) {
        return;
      }
      String apiResponse = body != null ? body.string() : null;
      String errorMessage = StringUtils.isNotBlank(apiResponse) ? apiResponse : responseMessage;
      throw new ServiceNowException(
          String.format("delete request failed with response code: %d with response: %s",
              responseCode, errorMessage));
    }
  }
}
