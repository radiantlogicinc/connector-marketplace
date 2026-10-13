package com.radiantlogic.utility;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.radiantlogic.model.WorkdayResponse;
import java.util.List;

/**
 * Utility class that holds application-wide constant values.
 *
 * <p>This class is not intended to be instantiated and serves as a centralized
 * location for reusable constant definitions used across the application.
 */
@SuppressWarnings("PMD.DataClass")
public final class Constants {

  /**
   * Endpoint path template used to request an OAuth access token.
   *
   * <p>The placeholder (%s) is replaced with the tenant identifier.
   */
  public static final String GENERATE_ACCESS_TOKEN_ENDPOINT = "ccx/oauth2/%s/token";
  /**
   * Endpoint path template for retrieving workers.
   */
  public static final String WORKERS_ENDPOINT_SUB_PATH = "ccx/api/staffing/v7/%s/workers";

  /**
   * Endpoint path template for retrieving worker details.
   */
  public static final String WORKER_DETAILS_ENDPOINT_SUB_PATH = "ccx/api/staffing/v7/%s/workers/%s";
  /**
   * Endpoint path template for retrieving jobs.
   */
  public static final String JOBS_ENDPOINT_SUB_PATH = "ccx/api/staffing/v7/%s/jobs";
  /**
   * Endpoint path template for retrieving job details.
   */
  public static final String JOB_DETAILS_ENDPOINT_SUB_PATH = "ccx/api/staffing/v7/%s/jobs/%s";
  /**
   * Endpoint path template for retrieving job postings.
   */
  public static final String JOB_POSTINGS_ENDPOINT_SUB_PATH =
      "ccx/api/recruiting/v4/%s/jobPostings";
  /**
   * Endpoint path template for retrieving job posting details.
   */
  public static final String JOB_POSTING_DETAILS_ENDPOINT_SUB_PATH =
      "ccx/api/recruiting/v4/%s/jobPostings/%s";

  /**
   * Endpoint path template for retrieving supervisory organizations.
   */
  public static final String SUPERVISORY_ORGANIZATION_ENDPOINT_SUB_PATH =
      "ccx/api/staffing/v7/%s/supervisoryOrganizations";
  /**
   * Endpoint path template for retrieving supervisory organizations  details.
   */
  public static final String SUPERVISORY_ORGANIZATION_DETAILS_ENDPOINT_SUB_PATH =
      "ccx/api/staffing/v7/%s/supervisoryOrganizations/%s";
  /**
   * Endpoint path template for retrieving given supervisory organization members.
   */
  public static final String SUPERVISORY_ORGANIZATION_MEMBERS_ENDPOINT_SUB_PATH =
      "ccx/api/staffing/v7/%s/supervisoryOrganizations/%s/members";

  /**
   * HTTP header key used to specify request content type.
   */
  public static final String HEADER_CONTENT_KEY = "Content-Type";

  /**
   * HTTP header value indicating form-encoded request payload.
   */
  public static final String HEADER_CONTENT_VALUE = "application/x-www-form-urlencoded";

  /**
   * OAuth parameter key used to specify the grant type in token requests.
   */
  public static final String GRANT_TYPE_KEY = "grant_type";

  /**
   * OAuth parameter key used to pass the JWT assertion in token requests.
   */
  public static final String ASSERTION_KEY = "assertion";

  /**
   * OAuth JWT bearer grant type identifier as per RFC 7523.
   */
  public static final String CODE_GRANT_TYPE =
      "urn:ietf:params:oauth:grant-type:jwt-bearer";

  /**
   * Configuration key used to identify the OAuth client ID.
   */
  public static final String CLIENT_ID_KEY = "client_id";

  /**
   * Configuration key used to identify the Workday instance name.
   */
  public static final String INSTANCE_NAME_KEY = "instance_name";

  /**
   * Configuration key used to specify the ISU username for authentication.
   */
  public static final String USERNAME_KEY = "username";

  /**
   * Configuration key used to specify the private key for JWT signing.
   */
  public static final String PRIVATE_KEY = "private_key";

  /**
   * Configuration key used to identify the tenant for authentication.
   */
  public static final String TENANT_NAME_KEY = "tenant";

  /**
   * Configuration key used to define pagination size for API responses.
   */
  public static final String PAGE_SIZE_KEY = "page_size";

  /**
   * Configuration key used to define the URL scheme.
   */
  public static final String URL_SCHEME_KEY = "scheme";

  /**
   * Configuration key used to define the service host.
   */
  public static final String URL_HOST_KEY = "host";

  /**
   * Configuration key used to define the service port.
   */
  public static final String URL_PORT_KEY = "port";
  /**
   * Key used to represent base search requests.
   */
  public static final String BASE_SEARCH_REQUEST_KEY = "BASE";

  /**
   * Schema object key for worker entity.
   */
  public static final String WORKER_ENTITY_KEY = "worker";
  /**
   * Schema object key for job entity.
   */
  public static final String JOB_ENTITY_KEY = "job";
  /**
   * Schema object key for job posting entity.
   */
  public static final String JOB_POSTING_ENTITY_KEY = "jobposting";
  /**
   * Schema object key for supervisory organization entity.
   */
  public static final String SUPERVISORY_ORGANIZATION_ENTITY_KEY = "supervisoryorganization";
  /**
   * Configuration key used to define the username for report basic authentication.
   */
  public static final String REPORT_USERNAME_KEY = "report_username";

  /**
   * Configuration key used to define the password for report basic authentication.
   */
  public static final String REPORT_PASSWORD_KEY = "report_password";
  /**
   * Schema object key for integration system users report entity.
   */
  public static final String INTEGRATION_SYSTEM_USERS_REPORT_ENTITY_KEY =
      "integrationsystemuser";
  /**
   * Schema object key for security groups report entity.
   */
  public static final String SECURITY_GROUPS_REPORT_ENTITY_KEY = "securitygroup";
  /**
   * Schema object key for security groups memberships report entity.
   */
  public static final String SECURITY_GROUPS_MEMBERSHIPS_REPORT_ENTITY_KEY =
      "securitygroupmembership";
  /**
   * Configuration key used to define the URL for Domains Report.
   */
  public static final String DOMAINS_REPORT_URL_KEY = "domains_report_url";
  /**
   * Configuration key used to define the URL for Business Processes Report.
   */
  public static final String BUSINESS_PROCESSES_REPORT_URL_KEY = "business_processes_report_url";
  /**
   * Configuration key used to define the URL for Integration System Users Report.
   */
  public static final String INTEGRATION_SYSTEM_USERS_REPORT_URL_KEY =
      "integration_system_users_report_url";
  /**
   * Configuration key used to define the URL for Security Groups Report.
   */
  public static final String SECURITY_GROUPS_REPORT_URL_KEY = "security_groups_report_url";
  /**
   * Configuration key used to define the URL for Security Groups Memberships Report.
   */
  public static final String SECURITY_GROUPS_MEMBERSHIPS_REPORT_URL_KEY =
      "security_groups_memberships_report_url";
  /**
   * Schema object key for domains report entity.
   */
  public static final String DOMAINS_REPORT_ENTITY_KEY = "domain";
  /**
   * Schema object key for business processes report entity.
   */
  public static final String BUSINESS_PROCESSES_REPORT_ENTITY_KEY = "businessprocess";
  /**
   * Gson instance used for JSON serialization and deserialization.
   */
  public static final Gson GSON = new Gson();

  /**
   * Configuration key used to define HTTP connection timeout.
   */
  public static final String CONNECTION_TIMEOUT_KEY_NAME = "connect_timeout";

  /**
   * Configuration key used to define HTTP read timeout.
   */
  public static final String READ_TIMEOUT_KEY_NAME = "read_timeout";

  /**
   * Configuration key used to define HTTP write timeout.
   */
  public static final String WRITE_TIMEOUT_KEY_NAME = "write_timeout";

  /**
   * Configuration key used to define retry delay.
   */
  public static final String RETRY_DELAY_KEY_NAME = "retry_delay";

  /**
   * Log message prefix used for Workday connector exceptions.
   */
  public static final String EXCEPTION_PREFIX = "Workday connector error : {}";
  /**
   * JSON key used for API response data section.
   */
  public static final String DATA_KEY = "data";

  /**
   * Exception message used when schema object type cannot be determined for a request.
   */
  public static final String UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE =
      "Unable to get schema object type for current search/Expecting IDDM to only ever send one "
          + "schema object per LDAP request";

  /**
   * Key used for schema objects section.
   */
  public static final String SCHEMA_OBJECTS_KEY = "schemaObjects";

  /**
   * Constant representing a size of one.
   */
  public static final int SIZE_ONE = 1;
  /**
   * HTTP header key used for authorization.
   */
  public static final String AUTHORIZATION_KEYWORD = "Authorization";

  /**
   * Query parameter key for limiting page size.
   */
  public static final String LIMIT_QUERY_PARAMETER = "limit";

  /**
   * Query parameter key for pagination offset.
   */
  public static final String OFFSET_QUERY_PARAMETER = "offset";

  /**
   * Exception message for cases where exactly one primary key is expected.
   */
  public static final String EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE =
      "expected single primary key attribute";

  /**
   * List of allowed primary key attributes.
   */
  public static final List<String> ALLOWED_PRIMARY_KEY_ATTRIBUTES =
      ImmutableList.of("workerid", "jobid", "jobpostingid", "supervisoryorganizationid",
          "username", "name", "workdayid");

  /**
   * Exception message for missing primary key attribute.
   */
  public static final String MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE =
      "missing required primary key attribute name";

  /**
   * Exception message prefix when a schema object cannot be found.
   */
  public static final String COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX =
      "Could not find schema object: ";

  /**
   * Gson instance configured with custom deserializer for Workday responses.
   */
  public static final Gson WORKDAY_RESPONSE_GSON = new GsonBuilder()
      .registerTypeAdapter(WorkdayResponse.class, new WorkdayResponseDeserializer<>())
      .create();

  /**
   * Error message used when connection to Workday fails due to authentication or connectivity
   * issues.
   */
  public static final String UNABLE_TO_CONNECT_WITH_WORKDAY_MESSAGE =
      "Unable to connect with Workday, please check the credentials";

  /**
   * Exception message prefix for invalid object types during data fetch.
   */
  public static final String INVALID_OBJECT_RECEIVED_FOR_DATA_FETCH_EXCEPTION_PREFIX =
      "Invalid object received for data fetch: %s";

  /**
   * Generic failure message for data retrieval operations.
   */
  public static final String DATA_FETCH_FAILED_MESSAGE = "Failed to get the data";

  private Constants() {
    // Utility class
  }
}
