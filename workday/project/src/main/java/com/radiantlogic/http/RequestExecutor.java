package com.radiantlogic.http;

import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.model.ResponseData;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Component responsible for executing HTTP requests and handling responses.
 *
 * <p>This class acts as a wrapper over the underlying HTTP client and provides
 * a centralized execution mechanism for sending requests, receiving responses,
 * and handling execution-related logging and errors.
 */
@Slf4j
public class RequestExecutor {

  /**
   * Underlying HTTP client used to execute network requests.
   */
  private final HttpClient httpClient;

  /**
   * Constructs a RequestExecutor with the provided HTTP client.
   *
   * @param httpClient the HTTP client used to execute requests
   */
  public RequestExecutor(final HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Executes the given HTTP request and returns the response data.
   *
   * <p>This method delegates execution to the underlying HTTP client and processes
   * the response into a {@link ResponseData} object. It may throw exceptions in
   * case of network failures or service-level errors.
   *
   * @param request the HTTP request to be executed
   * @return the response data containing HTTP status and response body
   * @throws IOException if a network or I/O error occurs during execution
   * @throws WorkdayException if the service returns an error response or invalid data
   */
  public ResponseData execute(final Request request)
      throws IOException, WorkdayException {
    try (Response response = httpClient.getOkHttpClient().newCall(request).execute();
         ResponseBody body = response.body()) {
      int responseCode = response.code();
      String responseMessage = response.message();
      log.debug("API response code is: {} with message: {}", responseCode, responseMessage);
      if (body != null) {
        return new ResponseData(body.string(), responseMessage, responseCode,
            response.headers());
      }
      throw new WorkdayException(
          String.format("API response message is: %s", responseMessage));
    }
  }
}
