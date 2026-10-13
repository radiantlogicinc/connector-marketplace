package com.radiantlogic.http;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.RETRY_DELAY_KEY_NAME;

import com.google.common.collect.ImmutableList;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Class to define retry mechanism for defined http status codes.
 */
@Slf4j
public class RetryInterceptor implements Interceptor {

  /**
   * Constant to define http codes for retry.
   */
  private static final List<Integer> RETRY_CODES = ImmutableList.of(429);
  /**
   * Constant to define max retries.
   */
  private static final int MAX_RETRIES = 5;
  /**
   * Constant to define initial backoff.
   */
  private static final long INITIAL_BACKOFF_MS = 1000;
  /**
   * Constant to define backoff multiplier.
   */
  private static final int MULTIPLIER = 2;
  /**
   * Constant to define max backoff delay.
   */
  private static final long MAX_BACKOFF_MS = 16_000;
  /**
   * Constant to define retry-after response header key.
   */
  private static final String RETRY_AFTER_HEADER_KEY = "Retry-After";
  /**
   * Field to define optional retry delay.
   */
  private final Long retryDelay;

  /**
   * Constructor to initialize instance members.
   */
  public RetryInterceptor(
      @Property(name = CUSTOM_DATASOURCE_PROPERTIES)
      final ReadOnlyProperties connectionProperties) {
    retryDelay = Optional.ofNullable(connectionProperties.get(RETRY_DELAY_KEY_NAME))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
  }

  @NonNull
  @Override
  public Response intercept(final Chain chain) throws IOException {
    Request request = chain.request();
    int retryAttempt = 0;
    while (true) {
      try {
        Response response = chain.proceed(request);
        if (!shouldRetry(response.code())) {
          return response;
        }
        retryAttempt++;
        if (retryAttempt > MAX_RETRIES) {
          response.close();
          break;
        }
        long delay = computeDelay(response, retryAttempt);
        log.warn("Retry {} for {} {} → HTTP {}, waiting {} ms", retryAttempt, request.method(),
            request.url(), response.code(), delay);
        response.close();
        sleep(delay);
      } catch (IOException ex) {
        retryAttempt++;
        if (retryAttempt > MAX_RETRIES) {
          throw ex;
        }
        long delay = computeDelay(null, retryAttempt);
        log.warn("Retry {} for {} {} → exception: {}, waiting {} ms", retryAttempt,
            request.method(), request.url(), ex.getMessage(), delay);
        sleep(delay);
      }
    }
    throw new IOException("Request failed after retries");
  }

  private boolean shouldRetry(final int code) {
    return RETRY_CODES.contains(code);
  }

  /**
   * Method to compute delay using: 1) Retry-After header (capped) 2) Configured retryDelay 3)
   * Exponential backoff.
   */
  private long computeDelay(final Response response, final int attempt) {
    Long retryAfterDelay = getRetryAfterDelay(response);
    if (retryAfterDelay != null) {
      return retryAfterDelay;
    }
    if (retryDelay != null && retryDelay > 0) {
      return retryDelay;
    }
    return computeBackoff(attempt);
  }

  /**
   * Method to parse Retry-After header and returns capped delay in ms, or null if header
   * absent/invalid.
   */
  private Long getRetryAfterDelay(final Response response) {
    if (response == null) {
      return null;
    }
    String retryAfter = response.header(RETRY_AFTER_HEADER_KEY);
    if (retryAfter == null) {
      return null;
    }
    long seconds;
    try {
      seconds = Long.parseLong(retryAfter.trim());
    } catch (NumberFormatException e) {
      log.info("Invalid Retry-After header value: {}", retryAfter);
      return null;
    }

    long millis = TimeUnit.SECONDS.toMillis(seconds);
    if (millis <= 0) {
      log.info("Ignoring non-positive Retry-After header value: {}", retryAfter);
      return null;
    }
    if (millis > MAX_BACKOFF_MS) {
      log.warn("Retry-After {} ms exceeds max {}, capping", millis, MAX_BACKOFF_MS);
      return MAX_BACKOFF_MS;
    }
    return millis;
  }

  private long computeBackoff(final int attempt) {
    long delay = INITIAL_BACKOFF_MS * (long) Math.pow(MULTIPLIER, attempt - 1);
    return Math.min(delay, MAX_BACKOFF_MS);
  }

  @SuppressWarnings("PMD.DoNotUseThreads")
  private void sleep(final long millis) throws IOException {
    try {
      TimeUnit.MILLISECONDS.sleep(millis);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new IOException("Retry interrupted", ie);
    }
  }
}
