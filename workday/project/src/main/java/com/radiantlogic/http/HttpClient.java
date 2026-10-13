package com.radiantlogic.http;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.CONNECTION_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.READ_TIMEOUT_KEY_NAME;
import static com.radiantlogic.utility.Constants.WRITE_TIMEOUT_KEY_NAME;

import com.radiantlogic.iddm.annotations.ManagedComponent;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;


/**
 * HTTP client component responsible for executing HTTP requests.
 *
 * <p>This class manages an underlying OkHttpClient instance configured with
 * connection settings such as timeouts and interceptors. It is designed to be used as a managed
 * component within the application lifecycle.
 */
@Slf4j
@Getter
@ManagedComponent
public class HttpClient {

  /**
   * Configured connection timeout for OkHttp requests.
   */
  private final Long connectTimeout;

  /**
   * Configured read timeout for OkHttp requests.
   */
  private final Long readTimeout;

  /**
   * Configured write timeout for OkHttp requests.
   */
  private final Long writeTimeout;
  /**
   * Constant to define HTTP client connect timeout value.
   */
  public static final int HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS = 30;
  /**
   * Constant to define HTTP client read timeout value.
   */
  public static final int HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS = 60;
  /**
   * Constant to define HTTP client write timeout value.
   */
  public static final int HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS = 60;

  /**
   * Shared OkHttp client instance used for API communication.
   */
  @Getter(onMethod_ = @SuppressFBWarnings
      (value = "EI_EXPOSE_REP", justification = "Expose internal representation safely"))
  private final OkHttpClient okHttpClient;

  /**
   * Constructs an HttpClient using the provided connection properties.
   *
   * <p>Initializes an underlying OkHttpClient instance with configurable connect, read, and write
   * timeouts. Also registers a retry interceptor to handle transient failures based on the provided
   * configuration.
   *
   * @param connectionProperties contains HTTP client configuration such as timeout values
   */
  public HttpClient(@Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                    final ReadOnlyProperties connectionProperties) {
    connectTimeout = getTimeout(connectionProperties, CONNECTION_TIMEOUT_KEY_NAME,
        HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS);
    readTimeout = getTimeout(connectionProperties, READ_TIMEOUT_KEY_NAME,
        HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS);
    writeTimeout = getTimeout(connectionProperties, WRITE_TIMEOUT_KEY_NAME,
        HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS);
    okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .addInterceptor(new RetryInterceptor(connectionProperties))
        .build();
  }

  private Long getTimeout(final ReadOnlyProperties connectionProperties,
                          final String key, final long defaultValue) {
    return Optional.ofNullable(connectionProperties.get(key))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .filter(value -> value > 0)
        .orElse(defaultValue);
  }
}
