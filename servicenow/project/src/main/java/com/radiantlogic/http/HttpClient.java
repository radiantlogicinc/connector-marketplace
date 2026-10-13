package com.radiantlogic.http;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.CONNECTION_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS;
import static com.radiantlogic.utility.Constants.HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS;
import static com.radiantlogic.utility.Constants.HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS;
import static com.radiantlogic.utility.Constants.READ_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.WRITE_TIMEOUT_KEY_NAME;

import com.radiantlogic.iddm.annotations.ManagedComponent;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * Class to define http client which will be shared across multiple https requests.
 */
@Slf4j
@Getter
@ManagedComponent
public final class HttpClient {

  /**
   * Field to store an optional connection timeout value.
   */
  private final Long connectTimeout;
  /**
   * Field to store an optional read timeout value.
   */
  private final Long readTimeout;
  /**
   * Field to store an optional write timeout value.
   */
  private final Long writeTimeout;
  /**
   * Field to store ok-http-client instance.
   */
  private final OkHttpClient okHttpClient;

  /**
   * Constructor to initialize instance members.
   *
   * @param connectionProperties Field containing config properties
   */
  public HttpClient(@Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                    final ReadOnlyProperties connectionProperties) {
    connectTimeout = Optional.ofNullable(connectionProperties.get(CONNECTION_TIMEOUT_KEY_NAME))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
    readTimeout = Optional.ofNullable(connectionProperties.get(READ_TIMEOUT_KEY_NAME))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
    writeTimeout = Optional.ofNullable(connectionProperties.get(WRITE_TIMEOUT_KEY_NAME))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
    okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(
            connectTimeout != null && connectTimeout > 0 ? connectTimeout :
                HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS,
            TimeUnit.SECONDS)
        .readTimeout(readTimeout != null && readTimeout > 0 ? readTimeout :
                HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS,
            TimeUnit.SECONDS)
        .writeTimeout(
            writeTimeout != null && writeTimeout > 0 ? writeTimeout :
                HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS,
            TimeUnit.SECONDS)
        .addInterceptor(new RetryInterceptor(connectionProperties))
        .build();
  }
}
