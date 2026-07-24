package com.radiantlogic.misc;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.BEARER_KEYWORD_WITH_SPACE;
import static com.radiantlogic.utility.Constants.CONNECTION_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS;
import static com.radiantlogic.utility.Constants.HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS;
import static com.radiantlogic.utility.Constants.HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS;
import static com.radiantlogic.utility.Constants.READ_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.TOKEN_EXPIRY_SUBTRACT_IN_SECONDS;
import static com.radiantlogic.utility.Constants.WRITE_TIMEOUT_KEY_NAME;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.utility.AccessToken;
import com.radiantlogic.utility.ResponseData;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will execute a request and will manage token generation & storage.
 */
@Slf4j
@Getter
@Setter
public class RequestExecutor {

  /**
   * This field will store token value with Bearer keyword prefixed.
   */
  private String tokenWithBearerPrefixed = StringUtils.EMPTY;
  /**
   * This method will store token expiry value.
   */
  private Instant expiration = Instant.ofEpochSecond(0L);
  /**
   * This object will be used to get url & payload for various create objects operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * This field will store an optional connection timeout value.
   */
  private Long connectTimeout;
  /**
   * This field will store an optional read timeout value.
   */
  private Long readTimeout;
  /**
   * This field will store an optional write timeout value.
   */
  private Long writeTimeout;
  /**
   * This field will contain connection properties.
   */
  private ReadOnlyProperties connectionProperties;

  /**
   * This constructor will initialize class's instance members.
   *
   * @param connectionProperties It will contain connection properties
   * @param requestBuilder       RequestBuilder class object
   */
  public RequestExecutor(
      @Property(name = CUSTOM_DATASOURCE_PROPERTIES) final ReadOnlyProperties connectionProperties,
      final RequestBuilder requestBuilder) {
    this.connectionProperties = connectionProperties;
    this.requestBuilder = requestBuilder;
    initializeAdditionalFields(connectionProperties);
  }

  private void initializeAdditionalFields(final ReadOnlyProperties connectionProperties) {
    if (connectionProperties.get(CONNECTION_TIMEOUT_KEY_NAME) != null) {
      connectTimeout =
          ((Number) connectionProperties.get(CONNECTION_TIMEOUT_KEY_NAME)).longValue();
    }
    if (connectionProperties.get(READ_TIMEOUT_KEY_NAME) != null) {
      readTimeout = ((Number) connectionProperties.get(READ_TIMEOUT_KEY_NAME)).longValue();
    }
    if (connectionProperties.get(WRITE_TIMEOUT_KEY_NAME) != null) {
      writeTimeout =
          ((Number) connectionProperties.get(WRITE_TIMEOUT_KEY_NAME)).longValue();
    }
  }

  /**
   * This method will execute a request.
   *
   * @param request Request object containing request details
   * @return ResponseData
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is null
   */
  public ResponseData execute(final Request request)
      throws IOException, CyberArkPrivilegeCloudException {
    try (Response response = getOkHttpClient().newCall(request).execute();
         ResponseBody body = response.body()) {
      int responseCode = response.code();
      String responseMessage = response.message();
      log.debug("api response code is:{} with message:{}", responseCode, responseMessage);
      if (body != null) {
        ResponseData responseData = new ResponseData();
        responseData.setApiResponse(body.string());
        responseData.setResponseMessage(responseMessage);
        responseData.setResponseCode(responseCode);
        return responseData;
      }
      throw new CyberArkPrivilegeCloudException(
          String.format("api response message is: %s", responseMessage));
    }
  }

  protected boolean isExpired() {
    // as part of defensive coding, reducing token expiry by 2 min while calculating its expiry
    return expiration.isBefore(Instant.now().minusSeconds(TOKEN_EXPIRY_SUBTRACT_IN_SECONDS));
  }

  /**
   * This method will check whether valid token is there, if not is new able to generate.
   *
   * @return boolean true or false
   */
  public boolean canConnect() {
    try {
      if (isExpired()) {
        generateToken();
      }
      return !isExpired();
    } catch (CyberArkPrivilegeCloudException | IOException e) {
      log.debug(EXCEPTION_PREFIX, e);
      return false;
    }
  }

  /**
   * This method will generate new token.
   *
   * @throws CyberArkPrivilegeCloudException In case api response is not OK
   * @throws IOException                     In case of connectivity error
   */
  public void generateToken() throws CyberArkPrivilegeCloudException, IOException {
    ResponseData responseData = execute(requestBuilder.getTokenRequest());
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      AccessToken accessToken = GOOGLE_JSON.fromJson(apiResponse, AccessToken.class);
      if (StringUtils.isNotBlank(accessToken.getToken())) {
        log.info("generated new token");
        tokenWithBearerPrefixed = BEARER_KEYWORD_WITH_SPACE.concat(accessToken.getToken());
        expiration = Instant.now().plusSeconds(accessToken.getExpiresIn());
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("Unable to generate access token: %s", apiResponse));
  }

  private OkHttpClient getOkHttpClient() {
    return new OkHttpClient().newBuilder()
        .connectTimeout(
            connectTimeout != null ? connectTimeout : HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS,
            TimeUnit.SECONDS)
        .readTimeout(readTimeout != null ? readTimeout : HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS,
            TimeUnit.SECONDS)
        .writeTimeout(
            writeTimeout != null ? writeTimeout : HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS,
            TimeUnit.SECONDS)
        .addInterceptor(new RetryInterceptor(connectionProperties)).build();
  }
}
