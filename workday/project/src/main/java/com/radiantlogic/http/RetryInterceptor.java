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
 * OkHttp interceptor responsible for handling retry logic for HTTP requests.
 *
 * <p>This interceptor retries failed requests based on configured retry conditions
 * such as specific HTTP status codes, retry-after headers, and exponential backoff strategy.
 * It is used to improve resilience against transient failures like rate limiting
 * or temporary network issues.
 */
@Slf4j
public class RetryInterceptor implements Interceptor {

  /**
   * HTTP status codes that are eligible for retry.
   *
   * <p>Currently includes 429 (Too Many Requests) to handle rate limiting scenarios.
   */
  private static final List<Integer> RETRY_CODES = ImmutableList.of(429);

  /**
   * Maximum number of retry attempts allowed before failing the request.
   */
  private static final int MAX_RETRIES = 5;

  /**
   * Initial delay (in milliseconds) before the first retry attempt.
   */
  private static final long INITIAL_BACKOFF_MS = 1000;

  /**
   * Multiplier used to calculate exponential backoff between retry attempts.
   */
  private static final int MULTIPLIER = 2;

  /**
   * Maximum allowed delay (in milliseconds) for any retry backoff calculation.
   */
  private static final long MAX_BACKOFF_MS = 16_000;

  /**
   * HTTP response header used to determine server-specified retry delay.
   */
  private static final String RETRY_AFTER_HEADER_KEY = "Retry-After";

  /**
   * Sentinel value indicating that no valid Retry-After header is present.
   */
  private static final long NO_RETRY_AFTER = -1L;

  /**
   * Optional fixed retry delay (in milliseconds) configured externally.
   *
   */
  private final Long retryDelay;

  /**
   * Constructs a RetryInterceptor using configuration provided in connection properties.
   *
   * <p>Initializes optional retry behavior such as fixed retry delay if configured.
   *
   * @param connectionProperties configuration properties used to initialize retry settings
   */
  public RetryInterceptor(
      @Property(name = CUSTOM_DATASOURCE_PROPERTIES)
      final ReadOnlyProperties connectionProperties) {
    retryDelay = Optional.ofNullable(connectionProperties.get(RETRY_DELAY_KEY_NAME))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
  }

  @Override
  public @NonNull Response intercept(final @NonNull Chain chain) throws IOException {
    Request request = chain.request();
    int retryAttempt = 0;
    while (true) {
      Response response = null;
      try {
        response = chain.proceed(request);
        // Caller is responsible for closing the returned response
        if (!shouldRetry(response.code())) {
          return response;
        }
        retryAttempt++;
        if (retryAttempt > MAX_RETRIES) {
          response.close();
          break;
        }
        long delay = computeDelay(response, retryAttempt);
        log.warn("Retry {} for {} {} HTTP {}, waiting {} ms",
            retryAttempt, request.method(), request.url(), response.code(), delay);
        response.close();
        sleep(delay);
      } catch (IOException ex) {
        if (response != null) {
          response.close();
        }
        retryAttempt++;
        if (retryAttempt > MAX_RETRIES) {
          throw ex;
        }
        long delay = computeDelay(null, retryAttempt);
        log.warn("Retry {} for {} {} exception: {}, waiting {} ms",
            retryAttempt, request.method(), request.url(), ex.getMessage(), delay);
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
    long retryAfterDelay = getRetryAfterDelay(response);
    if (retryAfterDelay != NO_RETRY_AFTER) {
      return retryAfterDelay;
    }
    if (retryDelay != null && retryDelay > 0) {
      return retryDelay;
    }
    return computeBackoff(attempt);
  }

  /**
   * Method to parse Retry-After header and returns capped delay in ms, or -1 if header
   * absent/invalid.
   */
  private long getRetryAfterDelay(final Response response) {
    if (response == null) {
      return NO_RETRY_AFTER;
    }
    String retryAfter = response.header(RETRY_AFTER_HEADER_KEY);
    if (retryAfter == null) {
      return NO_RETRY_AFTER;
    }
    try {
      long seconds = Long.parseLong(retryAfter.trim());
      long millis = TimeUnit.SECONDS.toMillis(seconds);
      if (millis <= 0) {
        log.info("Ignoring non-positive Retry-After header value: {}", retryAfter);
        return NO_RETRY_AFTER;
      }
      if (millis > MAX_BACKOFF_MS) {
        log.warn("Retry-After {} ms exceeds max {}, capping",
            millis, MAX_BACKOFF_MS);
        return MAX_BACKOFF_MS;
      }
      return millis;
    } catch (NumberFormatException e) {
      log.info("Invalid Retry-After header value: {}", retryAfter);
      return NO_RETRY_AFTER;
    }
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