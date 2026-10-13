package com.radiantlogic.http;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.ASSERTION_KEY;
import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.BUSINESS_PROCESSES_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.CLIENT_ID_KEY;
import static com.radiantlogic.utility.Constants.CODE_GRANT_TYPE;
import static com.radiantlogic.utility.Constants.DOMAINS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.GENERATE_ACCESS_TOKEN_ENDPOINT;
import static com.radiantlogic.utility.Constants.GRANT_TYPE_KEY;
import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.HEADER_CONTENT_KEY;
import static com.radiantlogic.utility.Constants.HEADER_CONTENT_VALUE;
import static com.radiantlogic.utility.Constants.INSTANCE_NAME_KEY;
import static com.radiantlogic.utility.Constants.INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.JOBS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.JOB_DETAILS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.JOB_POSTINGS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.JOB_POSTING_DETAILS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.LIMIT_QUERY_PARAMETER;
import static com.radiantlogic.utility.Constants.OFFSET_QUERY_PARAMETER;
import static com.radiantlogic.utility.Constants.PAGE_SIZE_KEY;
import static com.radiantlogic.utility.Constants.PRIVATE_KEY;
import static com.radiantlogic.utility.Constants.REPORT_PASSWORD_KEY;
import static com.radiantlogic.utility.Constants.REPORT_USERNAME_KEY;
import static com.radiantlogic.utility.Constants.SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.SECURITY_GROUPS_REPORT_URL_KEY;
import static com.radiantlogic.utility.Constants.SUPERVISORY_ORGANIZATION_DETAILS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.TENANT_NAME_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_WORKDAY_MESSAGE;
import static com.radiantlogic.utility.Constants.URL_HOST_KEY;
import static com.radiantlogic.utility.Constants.URL_PORT_KEY;
import static com.radiantlogic.utility.Constants.URL_SCHEME_KEY;
import static com.radiantlogic.utility.Constants.USERNAME_KEY;
import static com.radiantlogic.utility.Constants.WORKERS_ENDPOINT_SUB_PATH;
import static com.radiantlogic.utility.Constants.WORKER_DETAILS_ENDPOINT_SUB_PATH;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonSyntaxException;
import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.model.AccessToken;
import com.radiantlogic.model.ResponseData;
import com.radiantlogic.utility.JwtUtil;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;


/**
 * Builder class responsible for constructing and managing request-related configuration.
 *
 * <p>Encapsulates all required and optional parameters needed to execute requests,
 * including authentication details, connection settings, and request execution context. It also
 * supports token management and request preparation for API interactions.
 */

@Slf4j
@Getter
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"})
public final class RequestBuilder {

  /**
   * Executor responsible for sending HTTP requests and receiving responses.
   */
  private final RequestExecutor requestExecutor;

  /**
   * Bearer token used for authentication along with its expiry time.
   *
   * <p>Automatically initialized with an empty token and an expired timestamp.
   */
  private BearerToken bearerToken =
      new BearerToken(StringUtils.EMPTY, Instant.ofEpochSecond(0L));

  /**
   * Logical instance name identifying the target system instance.
   */
  private final String instanceName;
  /**
   * Tenant identifier used for authentication with the target system.
   */
  private final String tenant;
  /**
   * Username used for authentication with the target system.
   */
  private final String userName;
  /**
   * Number of records to fetch per page in paginated API responses.
   */
  private final int pageSize;
  /**
   * Private key used for signing authentication tokens.
   */
  @Getter(AccessLevel.NONE)
  private final String privateKey;
  /**
   * Client identifier used for generating and validating authentication tokens.
   */
  @Getter(AccessLevel.NONE)
  private final String clientId;

  /**
   * URL scheme used for constructing request endpoints.
   */
  private final String scheme;
  /**
   * Host name of the target service endpoint.
   */
  private final String host;
  /**
   * Port number of the target service endpoint, if explicitly configured.
   */
  private final Long port;
  /**
   * Signed JWT token generated for authentication with the remote service.
   */
  private String signedJwt;
  /**
   * Static JSON representation of the JWT header used for token generation. Specifies the signing
   * algorithm (RS256) and token type (JWT).
   */
  public static final String JWT_HEADER_JSON = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
  /**
   * Template for JWT payload JSON used during token creation.
   *
   * <p>Contains placeholders for issuer, subject, audience, issued-at time, and expiration time.
   * The final audience URL is constructed using the Workday tenant and instance details.
   */
  public static final String JWT_PAYLOAD_JSON = "{"
      + "\"iss\":\"%s\","
      + "\"sub\":\"%s\","
      + "\"aud\":\"https://%s.workday.com/ccx/oauth2/%s/token\","
      + "\"iat\": %d,"
      + "\"exp\": %d"
      + "}";

  /**
   * Prefix used for HTTP Authorization header when sending bearer tokens.
   */
  public static final String BEARER_KEYWORD_WITH_SPACE = "Bearer ";
  /**
   * Default URL scheme used for constructing service endpoints.
   */
  public static final String URL_SCHEME = "https";
  /**
   * Template for constructing the base Workday service URL using the instance name.
   */
  public static final String BASE_URL_PREFIX = "%s.workday.com";
  /**
   * Buffer time (in seconds) subtracted from the actual token expiry to proactively treat the token
   * as expired before its actual expiration.
   *
   * <p>This helps avoid edge cases where a token expires during an ongoing request.
   */
  public static final int TOKEN_EXPIRY_BUFFER_SECONDS = 120;
  /**
   * Error message template used when a required connection property is missing.
   *
   * <p>The placeholder (%s) is replaced with the name of the missing property.
   */
  public static final String MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX =
      "Missing required connection property: %s";
  /**
   * Log message used when the authentication response is invalid or cannot be parsed, which may
   * indicate that the Workday instance is in hibernation state.
   */
  private static final String INVALID_RESPONSE_MESSAGE =
      "Invalid response received, please check whether instance is not in hibernate state";
  /**
   * Error message logged when JWT generation or signing fails due to an invalid private key or
   * signing configuration issue.
   */
  private static final String JWT_SIGNING_FAILED_MESSAGE =
      "JWT signing failed: verify the configured private key and other configurations.";
  /**
   * URL for Integration System Users Report.
   */
  private final String integrationSystemUsersReportUrl;
  /**
   * URL for Security Groups Report.
   */
  private final String securityGroupsReportUrl;
  /**
   * URL for Security Groups Memberships Report.
   */
  private final String securityGroupsMembershipsReportUrl;
  /**
   * URL for Domains Report.
   */
  private final String domainsReportUrl;
  /**
   * URL for Business Processes Report.
   */
  private final String businessProcessesReportUrl;
  /**
   * Username for report basic authentication.
   */
  private final String reportUsername;
  /**
   * Password for report basic authentication.
   */
  @Getter(AccessLevel.NONE)
  private final String reportPassword;

  /**
   * Constructs a RequestBuilder by initializing all required configuration from the provided
   * connection properties and request executor.
   *
   * <p>This constructor extracts mandatory and optional configuration values such as
   * authentication details, tenant information, and endpoint settings, and prepares the builder for
   * executing authenticated requests. Optional fields are added purposefully to support tests.
   *
   * @param connectionProperties contains configuration properties required to initialize the
   *                             request builder
   * @param requestExecutor      executor responsible for executing HTTP requests
   */
  public RequestBuilder(
      @NonNull @Property(name = CUSTOM_DATASOURCE_PROPERTIES) final
      ReadOnlyProperties connectionProperties,
      @NonNull final RequestExecutor requestExecutor) {
    this.requestExecutor = requestExecutor;
    instanceName = Objects.requireNonNull(connectionProperties.get(INSTANCE_NAME_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, INSTANCE_NAME_KEY))
        .toString();
    userName = Objects.requireNonNull(connectionProperties.get(USERNAME_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, USERNAME_KEY))
        .toString();
    privateKey = Objects.requireNonNull(connectionProperties.get(PRIVATE_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, PRIVATE_KEY))
        .toString();
    tenant = Objects.requireNonNull(connectionProperties.get(TENANT_NAME_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, TENANT_NAME_KEY))
        .toString();
    clientId = Objects.requireNonNull(connectionProperties.get(CLIENT_ID_KEY),
            String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, CLIENT_ID_KEY))
        .toString();
    pageSize = Integer.parseInt(Objects.requireNonNull(connectionProperties.get(PAGE_SIZE_KEY),
        String.format(MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, PAGE_SIZE_KEY)).toString());
    scheme = Optional.ofNullable(connectionProperties.get(URL_SCHEME_KEY))
        .map(Object::toString)
        .orElse("");
    host = Optional.ofNullable(connectionProperties.get(URL_HOST_KEY))
        .map(Object::toString)
        .orElse("");
    port = Optional.ofNullable(connectionProperties.get(URL_PORT_KEY))
        .filter(Number.class::isInstance)
        .map(value -> ((Number) value).longValue())
        .orElse(null);
    reportUsername =
        Objects.requireNonNull(connectionProperties.get(REPORT_USERNAME_KEY))
            .toString();
    reportPassword =
        Objects.requireNonNull(connectionProperties.get(REPORT_PASSWORD_KEY))
            .toString();
    integrationSystemUsersReportUrl =
        Objects.requireNonNull(connectionProperties.get(INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY),
                String.format(
                    MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX,
                    INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY))
            .toString();
    securityGroupsReportUrl =
        Objects.requireNonNull(connectionProperties.get(SECURITY_GROUPS_REPORT_URL_KEY),
                String.format(
                    MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX,
                    SECURITY_GROUPS_REPORT_URL_KEY))
            .toString();
    securityGroupsMembershipsReportUrl =
        Objects.requireNonNull(connectionProperties.get(SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY),
                String.format(
                    MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX,
                    SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY))
            .toString();
    domainsReportUrl =
        Objects.requireNonNull(connectionProperties.get(DOMAINS_REPORT_URL_KEY), String.format(
                MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, DOMAINS_REPORT_URL_KEY
            ))
            .toString();
    businessProcessesReportUrl =
        Objects.requireNonNull(connectionProperties.get(BUSINESS_PROCESSES_REPORT_URL_KEY),
                String.format(
                    MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX, BUSINESS_PROCESSES_REPORT_URL_KEY
                ))
            .toString();
  }

  /**
   * Validates connectivity by ensuring a valid bearer token is available.
   *
   * <p>If the current token is expired, this method attempts to generate a new access token. It
   * returns {@code true} if a valid (non-expired) token is available after the check, otherwise
   * returns {@code false}. Any exceptions during token generation or validation are logged and
   * treated as a connectivity failure.
   *
   * @return {@code true} if a valid token is available, otherwise {@code false}
   */
  public boolean canConnectWithValidToken() {
    try {
      if (bearerToken.isExpired()) {
        generateAccessToken();
      }
      return !bearerToken.isExpired();
    } catch (JsonProcessingException | JsonSyntaxException e) {
      log.error(INVALID_RESPONSE_MESSAGE, e);
      return false;
    } catch (NoSuchAlgorithmException
             | SignatureException
             | InvalidKeyException
             | InvalidKeySpecException e) {
      log.error(JWT_SIGNING_FAILED_MESSAGE, e);
      return false;
    } catch (WorkdayException | IOException e) {
      log.error(EXCEPTION_PREFIX, e.getMessage(), e);
      return false;
    }
  }

  private void generateAccessToken()
      throws WorkdayException, NoSuchAlgorithmException, SignatureException,
      InvalidKeyException,
      IOException, InvalidKeySpecException {
    PrivateKey loadedPrivateKey = JwtUtil.loadPrivateKey(privateKey);
    signedJwt = generateJwtToken(loadedPrivateKey);
    ResponseData responseData = requestExecutor.execute(getTokenRequest());
    String apiResponse = responseData.getApiResponse();
    if (responseData.getResponseCode() == HttpStatus.OK.asInt()) {
      AccessToken accessToken = GSON.fromJson(apiResponse, AccessToken.class);
      if (accessToken != null && StringUtils.isNotBlank(accessToken.getToken())) {
        try {
          bearerToken = new BearerToken(
              BEARER_KEYWORD_WITH_SPACE.concat(accessToken.getToken()),
              Instant.now().plusSeconds(
                  JwtUtil.getExpiryInstant(accessToken.getToken()))
          );
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
          throw new WorkdayException("Invalid JWT token received from Workday", e);
        }
        log.info("generated new token");
      } else {
        throw new WorkdayException("Access token is missing or blank in successful response");
      }
    } else {
      throw new WorkdayException(
          "Token request failed with HTTP "
              + responseData.getResponseCode()
              + (StringUtils.isNotBlank(apiResponse)
              ? ". Response: " + apiResponse
              : ""));
    }
  }

  private String generateJwtToken(final PrivateKey loadedPrivateKey)
      throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
    long issuedAt = Instant.now().getEpochSecond();
    long expiration = issuedAt + 300;
    String payloadJson = String.format(JWT_PAYLOAD_JSON, clientId,
        userName, instanceName, tenant, issuedAt, expiration);
    String encodedHeader = JwtUtil.getEncodedValue(JWT_HEADER_JSON);
    String encodedPayload = JwtUtil.getEncodedValue(payloadJson);
    String signingInput = encodedHeader + "." + encodedPayload;
    String signature = JwtUtil.sign(signingInput, loadedPrivateKey);
    return signingInput + "." + signature;
  }

  /**
   * Builds and returns the base URL builder for the target service endpoint.
   *
   * <p>The URL is constructed using configured scheme, host, and optional port.
   * If scheme or host are not explicitly configured, default values are used. The resulting builder
   * can be further extended with path segments or query parameters.
   *
   * @return a configured {@link HttpUrl.Builder} representing the base service URL
   */
  public HttpUrl.Builder getBaseUrl() {
    HttpUrl.Builder builder =
        new HttpUrl.Builder().scheme(
                StringUtils.isNotBlank(scheme) ? scheme : URL_SCHEME)
            .host(
                StringUtils.isNotBlank(host)
                    ? host
                    : String.format(BASE_URL_PREFIX, instanceName));
    if (port != null && port > 0) {
      builder.port(port.intValue());
    }
    return builder;
  }

  /**
   * Attempts to establish a connection to Workday by generating a new access token.
   *
   * <p>On success, the cached bearer token is refreshed and stored for
   * subsequent authenticated API requests.
   *
   * @return {@code true} if the connection is established successfully; {@code false} otherwise.
   *
   */
  public boolean canEstablishConnection() {
    try {
      generateAccessToken();
      return true;
    } catch (JsonProcessingException | JsonSyntaxException e) {
      log.error(INVALID_RESPONSE_MESSAGE, e);
      return false;
    } catch (NoSuchAlgorithmException
             | SignatureException
             | InvalidKeyException
             | InvalidKeySpecException e) {
      log.error(JWT_SIGNING_FAILED_MESSAGE, e);
      return false;
    } catch (WorkdayException | IOException e) {
      log.error(EXCEPTION_PREFIX, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Represents a bearer authentication token along with its expiration time.
   *
   * <p>This class is used to track the validity of an access token and determine whether it has
   * expired before making authenticated requests. It is intentionally package-private/static for
   * controlled usage and testability.
   */
  @AllArgsConstructor
  static class BearerToken {

    /**
     * The bearer access token used for authentication in API requests.
     */
    final String token;
    /**
     * The timestamp indicating when this token expires and becomes invalid.
     */
    final Instant expiration;

    boolean isExpired() {
      /*
       * Advance the comparison baseline by TOKEN_EXPIRY_BUFFER_SECONDS
       * so the token is treated as expired
       * 2 minutes before its actual expiry, preventing mid-request expiry on slow calls.
       */
      return expiration.isBefore(
          Instant.now().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
    }
  }

  private Request getTokenRequest() {
    return new Request.Builder()
        .addHeader(HEADER_CONTENT_KEY, HEADER_CONTENT_VALUE)
        .url(getAccessTokenUrl())
        .post(getFormBodyForAccessToken())
        .build();
  }

  private HttpUrl getAccessTokenUrl() {
    HttpUrl tokenUrl =
        getBaseUrl()
            .addPathSegments(
                String.format(GENERATE_ACCESS_TOKEN_ENDPOINT, tenant))
            .build();
    log.debug("url to generate access token is: {}", tokenUrl.url());
    return tokenUrl;
  }

  private FormBody getFormBodyForAccessToken() {
    return new FormBody.Builder()
        .add(GRANT_TYPE_KEY, CODE_GRANT_TYPE)
        .add(ASSERTION_KEY, signedJwt)
        .build();
  }

  /**
   * Builds a request for retrieving worker data.
   *
   * @param workerId the worker identifier used to fetch a specific worker record
   * @param offset   the pagination offset used for retrieving subsequent results
   * @return a configured request object for worker data retrieval
   * @throws WorkdayException if credentials are invalid, a valid token is not available, or the
   *                          next link URL is invalid or malformed
   */
  public Request getWorkersRequest(final String workerId, final int offset)
      throws WorkdayException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getWorkersUrl(workerId, offset)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.token).build();
  }

  private void checkWhetherValidTokenExist() throws WorkdayException {
    if (!canConnectWithValidToken()) {
      throw new WorkdayException(UNABLE_TO_CONNECT_WITH_WORKDAY_MESSAGE);
    }
  }

  private HttpUrl getWorkersUrl(final String workerId, final int offset) {
    HttpUrl workersUrl;
    if (StringUtils.isBlank(workerId)) {
      workersUrl =
          getBaseUrl()
              .addPathSegments(String.format(WORKERS_ENDPOINT_SUB_PATH, tenant))
              .addQueryParameter(LIMIT_QUERY_PARAMETER, String.valueOf(pageSize))
              .addQueryParameter(OFFSET_QUERY_PARAMETER, String.valueOf(offset))
              .build();

    } else {

      workersUrl = getBaseUrl()
          .addPathSegments(String.format(WORKER_DETAILS_ENDPOINT_SUB_PATH, tenant, workerId))
          .build();

    }
    log.debug("workers url is {} ", workersUrl);
    return workersUrl;
  }

  /**
   * Builds a request for retrieving job data.
   *
   * @param jobId  identifier of the job to retrieve
   * @param offset pagination offset for the request
   * @return request object used to fetch job data
   * @throws WorkdayException if authentication fails, credentials are invalid, or the next-link URL
   *                          is malformed or has an invalid base URL
   */
  public Request getJobsRequest(final String jobId, final int offset)
      throws WorkdayException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getJobsUrl(jobId, offset)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.token).build();
  }

  private HttpUrl getJobsUrl(final String jobId, final int offset) {
    final HttpUrl jobsUrl;
    if (StringUtils.isBlank(jobId)) {
      jobsUrl =
          getBaseUrl()
              .addPathSegments(String.format(JOBS_ENDPOINT_SUB_PATH, tenant))
              .addQueryParameter(LIMIT_QUERY_PARAMETER, String.valueOf(pageSize))
              .addQueryParameter(OFFSET_QUERY_PARAMETER, String.valueOf(offset))
              .build();

    } else {

      jobsUrl = getBaseUrl()
          .addPathSegments(String.format(JOB_DETAILS_ENDPOINT_SUB_PATH, tenant, jobId))
          .build();

    }
    log.debug("jobs url is {} ", jobsUrl);
    return jobsUrl;
  }

  /**
   * Creates a request for retrieving job posting data from Workday.
   *
   * <p>The generated request includes the specified job posting identifier
   * and pagination offset, and is used to invoke the Workday job postings endpoint.</p>
   *
   * @param jobPostingId unique identifier of the job posting to retrieve; if null or empty, the
   *                     request may return multiple job postings depending on the implementation
   * @param offset       zero-based offset used for paginated retrieval of results
   * @return a configured {@link Request} for retrieving job posting data from Workday
   * @throws WorkdayException if the request cannot be constructed due to invalid configuration or
   *                          request parameters
   */
  public Request getJobPostingsRequest(final String jobPostingId, final int offset)
      throws WorkdayException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(getJobPostingsUrl(jobPostingId, offset)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.token).build();
  }

  private HttpUrl getJobPostingsUrl(final String jobPostingId, final int offset) {
    final HttpUrl jobPostingsUrl;
    if (StringUtils.isBlank(jobPostingId)) {
      jobPostingsUrl =
          getBaseUrl()
              .addPathSegments(String.format(JOB_POSTINGS_ENDPOINT_SUB_PATH, tenant))
              .addQueryParameter(LIMIT_QUERY_PARAMETER, String.valueOf(pageSize))
              .addQueryParameter(OFFSET_QUERY_PARAMETER, String.valueOf(offset))
              .build();

    } else {

      jobPostingsUrl = getBaseUrl()
          .addPathSegments(
              String.format(JOB_POSTING_DETAILS_ENDPOINT_SUB_PATH, tenant, jobPostingId))
          .build();

    }
    log.debug("job postings url is {} ", jobPostingsUrl);
    return jobPostingsUrl;
  }

  /**
   * Creates a Workday request for retrieving Supervisory Organization data.
   *
   * <p>If a Supervisory Organization identifier is provided, the request is
   * configured to retrieve the corresponding Supervisory Organization. Otherwise, the request
   * retrieves Supervisory Organizations starting at the specified offset.
   *
   * @param supervisoryOrganizationId the unique identifier of the Supervisory Organization to
   *                                  retrieve; may be {@code null} to retrieve multiple
   *                                  organizations
   * @param offset                    the starting offset used for paginated retrieval
   * @return a configured Workday request for retrieving Supervisory Organization data
   * @throws WorkdayException if the request cannot be constructed due to invalid configuration or
   *                          request parameters
   */
  public Request getSupervisoryOrganizationsRequest(final String supervisoryOrganizationId,
                                                    final int offset)
      throws WorkdayException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(
            getSupervisoryOrganizationsUrl(supervisoryOrganizationId, offset)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.token).build();
  }

  private HttpUrl getSupervisoryOrganizationsUrl(final String supervisoryOrganizationId,
                                                 final int offset) {
    final HttpUrl supervisoryOrganizationsUrl;
    if (StringUtils.isBlank(supervisoryOrganizationId)) {
      supervisoryOrganizationsUrl =
          getBaseUrl()
              .addPathSegments(String.format(SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH, tenant))
              .addQueryParameter(LIMIT_QUERY_PARAMETER, String.valueOf(pageSize))
              .addQueryParameter(OFFSET_QUERY_PARAMETER, String.valueOf(offset))
              .build();

    } else {

      supervisoryOrganizationsUrl = getBaseUrl()
          .addPathSegments(String.format(SUPERVISORY_ORGANIZATION_DETAILS_ENDPOINT_SUB_PATH, tenant,
              supervisoryOrganizationId))
          .build();

    }
    log.debug("supervisory Organizations url is {} ", supervisoryOrganizationsUrl);
    return supervisoryOrganizationsUrl;
  }

  /**
   * Creates a Workday request for retrieving members of a Supervisory Organization.
   *
   * <p>The request is configured to retrieve workers or organization members
   * associated with the specified Supervisory Organization. The provided offset is used to support
   * paginated retrieval of member records.
   *
   * @param supervisoryOrganizationsId the unique identifier of the Supervisory Organization whose
   *                                   members are to be retrieved
   * @param offset                     the starting offset used for paginated retrieval
   * @param limit                      no of records to be fetched in each execution
   * @return a configured Workday request for retrieving Supervisory Organization member data
   * @throws WorkdayException if the request cannot be constructed due to invalid configuration or
   *                          request parameters
   */
  public Request getSupervisoryOrganizationMembersRequest(final String supervisoryOrganizationsId,
                                                          final int offset, final int limit)
      throws WorkdayException {
    checkWhetherValidTokenExist();
    return new Request.Builder().url(
            getSupervisoryOrganizationMembersUrl(supervisoryOrganizationsId, offset, limit)).get()
        .header(AUTHORIZATION_KEYWORD, bearerToken.token).build();
  }

  private HttpUrl getSupervisoryOrganizationMembersUrl(final String workerId, final int offset,
                                                       final int limit) {
    final HttpUrl supervisoryOrganizationMembersUrl;
    supervisoryOrganizationMembersUrl =
        getBaseUrl()
            .addPathSegments(
                String.format(SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH, tenant, workerId))
            .addQueryParameter(LIMIT_QUERY_PARAMETER, String.valueOf(limit))
            .addQueryParameter(OFFSET_QUERY_PARAMETER, String.valueOf(offset))
            .build();


    log.debug("supervisory Organizations Members url is {} ", supervisoryOrganizationMembersUrl);
    return supervisoryOrganizationMembersUrl;
  }

  /**
   * Builds a request for retrieving report data from the configured report URL.
   *
   * <p>If report username and password are configured, basic authentication is
   * applied. Otherwise, no Authorization header is added to the request.
   *
   * @param reportUrl the URL from which to fetch the report
   * @return a configured request object for report data retrieval
   */
  public Request getReportRequest(final String reportUrl) {
    Request.Builder requestBuilder = new Request.Builder().url(reportUrl).get();

    if (StringUtils.isNotBlank(reportUsername) && StringUtils.isNotBlank(reportPassword)) {
      String credentials = Credentials.basic(reportUsername, reportPassword);
      requestBuilder.header(AUTHORIZATION_KEYWORD, credentials);
    }
    return requestBuilder.build();
  }

}
