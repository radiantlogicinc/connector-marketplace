package com.radiantlogic.misc;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.DATA_FETCH_FAILED_MESSAGE;
import static com.radiantlogic.utility.Constants.INITIAL_BACKOFF_MILLIS;
import static com.radiantlogic.utility.Constants.MAX_RETRIES;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.RETRY_CODES;
import static com.radiantlogic.utility.Constants.RETRY_DELAY_KEY_NAME;

import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class will handle retry mechanism for defined http status codes.
 */
@Slf4j
public class RetryInterceptor implements Interceptor {

  /**
   * This field will contain an optional retry delay interval for an api call.
   */
  private Long retryDelay;

  /**
   * This constructor will initialize instance members.
   *
   * @param connectionProperties It will contain config properties
   */
  public RetryInterceptor(@Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                          final ReadOnlyProperties connectionProperties) {
    if (connectionProperties.get(RETRY_DELAY_KEY_NAME) != null) {
      retryDelay = ((Number) connectionProperties.get(RETRY_DELAY_KEY_NAME)).longValue();
    }
  }

  @NonNull
  @Override
  @SuppressWarnings(PMD_KEY)
  public Response intercept(final Chain chain) throws IOException {
    Request request = chain.request();
    int retryCount = 0;
    long currentBackoffMillis = INITIAL_BACKOFF_MILLIS;
    while (retryCount < MAX_RETRIES) {
      try {
        Response response = chain.proceed(request);
        int responseCode = response.code();
        if (RETRY_CODES.contains(responseCode)) {
          retryCount++;
          currentBackoffMillis = retryDelay != null ? retryDelay :
              (long) (INITIAL_BACKOFF_MILLIS * Math.pow(3, retryCount));
          log.info("retry request received with code: {}, retrying in: {} milliseconds",
              responseCode, currentBackoffMillis);
          TimeUnit.MILLISECONDS.sleep(currentBackoffMillis);
          response.close();
        } else {
          return response;
        }
      } catch (IOException e) {
        try {
          TimeUnit.MILLISECONDS.sleep(currentBackoffMillis);
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        }
        retryCount++;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    throw new IOException(DATA_FETCH_FAILED_MESSAGE);
  }
}
