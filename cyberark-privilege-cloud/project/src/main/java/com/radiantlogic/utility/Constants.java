package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import okhttp3.MediaType;

/**
 * This class will contain constants used by the connector.
 */
@SuppressWarnings(PMD_KEY)
public final class Constants {

  private Constants() {
  }

  /**
   * This constant will define URL scheme value.
   */
  public static final String URL_SCHEME = "https";
  /**
   * This constant will define base url prefix value.
   */
  public static final String BASE_URL_PREFIX = "%s.id.cyberark.cloud";
  /**
   * This constant will define access token endpoint value.
   */
  public static final String GENERATE_ACCESS_TOKEN_ENDPOINT = "oauth2/platformtoken";
  /**
   * This constant will define redrock endpoint value.
   */
  public static final String RED_ROCK_ENDPOINT = "RedRock/query";
  /**
   * This constant will define delete role endpoint value.
   */
  public static final String DELETE_ROLES_ENDPOINT = "saasManage/DeleteRoles";
  /**
   * This constant will define delete user endpoint value.
   */
  public static final String DELETE_USERS_ENDPOINT = "UserMgmt/RemoveUsers";
  /**
   * This constant will define get role members endpoint value.
   */
  public static final String GET_ROLE_MEMBERS_ENDPOINT = "Roles/GetRoleMembers";
  /**
   * This constant will define update user endpoint value.
   */
  public static final String UPDATE_USER_ENDPOINT = "User/UpdateProfile";
  /**
   * This constant will define update role endpoint value.
   */
  public static final String UPDATE_ROLE_ENDPOINT = "Roles/UpdateRole";
  /**
   * This constant will define create user endpoint value.
   */
  public static final String CREATE_USER_ENDPOINT = "CDirectoryService/CreateUser";
  /**
   * This constant will define create role endpoint value.
   */
  public static final String CREATE_ROLE_ENDPOINT = "Roles/StoreRole";
  /**
   * This constant will define content type key value.
   */
  public static final String CONTENT_TYPE_KEY = "Content-Type";
  /**
   * This constant will define url encoded content type key value.
   */
  public static final String URL_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";
  /**
   * This constant will define grant type key value.
   */
  public static final String GRANT_TYPE_KEY = "grant_type";
  /**
   * This constant will define client credentials key value.
   */
  public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
  /**
   * This constant will define client id key value.
   */
  public static final String CLIENT_ID_KEY = "client_id";
  /**
   * This constant will define client secret key value.
   */
  public static final String CLIENT_SECRET_KEY = "client_secret";
  /**
   * This constant will define identity tenant id key value.
   */
  public static final String IDENTITY_TENANT_ID_KEY = "identity_tenant_id";
  /**
   * This constant will define page size key value.
   */
  public static final String PAGE_SIZE_KEY = "page_size";
  /**
   * This constant will define missing required connection property prefix message value.
   */
  public static final String MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX =
      "Missing required connection property: %s";
  /**
   * This constant will define exception prefix value.
   */
  public static final String EXCEPTION_PREFIX = "Exception is: ";
  /**
   * This constant will define base search key value.
   */
  public static final String BASE_SEARCH_REQUEST_KEY = "BASE";
  /**
   * This constant will define failed to get data message value.
   */
  public static final String DATA_FETCH_FAILED_MESSAGE = "Failed to get the data";
  /**
   * This constant will define bearer key with space value.
   */
  public static final String BEARER_KEYWORD_WITH_SPACE = "Bearer ";
  /**
   * This constant will define authorization key value.
   */
  public static final String AUTHORIZATION_KEYWORD = "Authorization";
  /**
   * This constant will define seconds to subtract while calculating token expiry.
   */
  public static final int TOKEN_EXPIRY_SUBTRACT_IN_SECONDS = 120;
  /**
   * This constant will define JSON media type.
   */
  public static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");
  /**
   * This constant will define HTTP client connect timeout value.
   */
  public static final int HTTP_CLIENT_CONNECT_TIMEOUT_VALUE_IN_SECONDS = 30;
  /**
   * This constant will define HTTP client read timeout value.
   */
  public static final int HTTP_CLIENT_READ_TIMEOUT_VALUE_IN_SECONDS = 60;
  /**
   * This constant will define HTTP client read timeout value.
   */
  public static final int HTTP_CLIENT_WRITE_TIMEOUT_VALUE_IN_SECONDS = 60;
  /**
   * This constant will define start index value to get substring.
   */
  public static final int START_INDEX = 6;
  /**
   * This constant will define max https retries value.
   */
  public static final int MAX_RETRIES = 3;
  /**
   * This constant will define initial delay of retry value.
   */
  public static final long INITIAL_BACKOFF_MILLIS = 5000;
  /**
   * This constant will define http codes for retry.
   */
  public static final List<Integer> RETRY_CODES = ImmutableList.of(429);
  /**
   * This constant will define entities on which various CRUD operations can be performed.
   */
  public static final List<String> ALLOWED_ENTITIES = ImmutableList.of("user", "role");
  /**
   * This constant will define user entity value.
   */
  public static final String USER_ENTITY_KEY = "user";
  /**
   * This constant will define role entity value.
   */
  public static final String ROLE_ENTITY_KEY = "role";
  /**
   * This constant will define system administrator key value.
   */
  public static final String SYSTEM_ADMINISTRATOR_ROLE_NAME_KEY = "System Administrator";
  /**
   * This constant will define attribute details log message value.
   */
  public static final String ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE =
      "attribute name is :{} with values:{} and modification type is:{}";
  /**
   * This constant will define port number for testing value.
   */
  public static final int PORT_NUMBER_FOR_TESTING = 50_000;
  /**
   * This constant will define HTTP scheme for testing value.
   */
  public static final String SCHEME_FOR_TESTING = "http";
  /**
   * This constant will define host for testing value.
   */
  public static final String HOST_FOR_TESTING = "localhost";
  /**
   * This constant will define schema objects key value.
   */
  public static final String SCHEMA_OBJECTS_KEY = "schemaObjects";
  /**
   * This constant will define size one value.
   */
  public static final int SIZE_ONE = 1;
  /**
   * This constant will define unable to connect with cyberark message value.
   */
  public static final String UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE =
      "Unable to connect with CyberArk Privilege Cloud, please check the credentials";
  /**
   * This constant will define get role by name payload in json string format.
   */
  public static final String GET_ROLE_BY_NAME_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from Roles where Name='%s'\",\n" + "    \"Args\": {\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define get role by id payload in json string format.
   */
  public static final String GET_ROLE_BY_ID_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from Roles where ID='%s'\",\n" + "    \"Args\": {\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define get roles by ids payload in json string format.
   */
  public static final String GET_ROLES_BY_IDS_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from Roles where ID IN (%s)\"\n" + "}";
  /**
   * This constant will define get roles payload in json string format.
   */
  public static final String GET_ROLES_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from Roles\",\n" + "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Name\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define get role members payload in json string format.
   */
  public static final String GET_ROLE_MEMBERS_PAYLOAD = "{\n" + "    \"name\": \"%s\"\n" + "}";
  /**
   * This constant will define get roles with members payload in json string format.
   */
  public static final String GET_ROLES_WITH_MEMBERS_PAYLOAD =
      "{\n" + "    \"Script\": \"@/lib/all_roles_with_members.js\",\n" + "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Name\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define get user by username payload in json string format.
   */
  public static final String GET_USER_BY_USERNAME_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from User where Username='%s'\",\n" + "    \"Args\": {\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define get user by id payload in json string format.
   */
  public static final String GET_USER_BY_ID_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from User where ID='%s'\",\n" + "    \"Args\": {\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define get users by ids payload in json string format.
   */
  public static final String GET_USERS_BY_IDS_PAYLOAD =
      "{\n" + "    \"Script\": \"select * from User where ID IN (%s)\"\n" + "}";
  /**
   * This constant will define get users payload in json string format.
   */
  public static final String GET_USERS_PAYLOAD =
      "{\n" + "    \"Script\": \"@@All Users\",\n" + "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Username\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * This constant will define delete role payload in json string format.
   */
  public static final String DELETE_ROLE_PAYLOAD = "[\n" + "    \"%s\"\n" + "]";
  /**
   * This constant will define delete user payload in json string format.
   */
  public static final String DELETE_USER_PAYLOAD =
      "{\n" + "    \"Users\": [\n" + "        \"%s\"\n" + "    ]\n" + "}";
  /**
   * This constant will define pmd key.
   */
  public static final String PMD_KEY = "PMD";
  /**
   * GSON constant with a custom JSON deserializer to parse CyberArk API responses.
   */
  public static final Gson GOOGLE_JSON = new GsonBuilder()
      .registerTypeAdapter(Record.class, new RecordDeserializer())
      .create();
  /**
   * This constant will define base url using subdomain.
   */
  public static final String SUBDOMAIN_BASE_URL_PREFIX = "%s.privilegecloud.cyberark.cloud";
  /**
   * This constant will define safe entity key.
   */
  public static final String SAFE_ENTITY_KEY = "safe";
  /**
   * This field will store get safes sub path.
   */
  public static final String GET_SAFES_SUB_PATH = "safes";
  /**
   * This field will store password vault sub path.
   */
  public static final String PASSWORD_VAULT_SUB_PATH = "PasswordVault";
  /**
   * This field will store api sub path.
   */
  public static final String API_SUB_PATH = "API";
  /**
   * This field will store get safe members sub path.
   */
  public static final String MEMBERS_SUB_PATH = "Members";
  /**
   * This field will store date time formatting style.
   */
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
  /**
   * This field will store server zone id.
   */
  public static final ZoneId ZONE = ZoneId.systemDefault();
  /**
   * This field will store microseconds to seconds convert divisor.
   */
  public static final long MICROSECONDS_TO_SECONDS_CONVERT_DIVISOR = 1_000_000;
  /**
   * This field will store forward slash key.
   */
  public static final String FORWARD_SLASH_KEY = "/";
  /**
   * This field will store limit key.
   */
  public static final String LIMIT_KEY = "limit";
  /**
   * This field will store use cache key.
   */
  public static final String USE_CACHE_KEY = "useCache";
  /**
   * This constant will define subdomain key value.
   */
  public static final String SUBDOMAIN_KEY = "subdomain";
  /**
   * This constant will define sort key value.
   */
  public static final String SORT_KEY = "sort";
  /**
   * This constant will define safes sorting field name.
   */
  public static final String SORT_SAFES_FIELD_NAME = "safeName";
  /**
   * This constant will define safe members sorting field name.
   */
  public static final String SORT_SAFE_MEMBERS_FIELD_NAME = "memberName";
  /**
   * This constant will define URL scheme key name.
   */
  public static final String URL_SCHEME_KEY = "scheme";
  /**
   * This constant will define URL host key name.
   */
  public static final String URL_HOST_KEY = "host";
  /**
   * This constant will define URL port key name.
   */
  public static final String URL_PORT_KEY = "port";
  /**
   * This constant will define connection timeout key name.
   */
  public static final String CONNECTION_TIMEOUT_KEY_NAME = "connect_timeout";
  /**
   * This constant will define read timeout key name.
   */
  public static final String READ_TIMEOUT_KEY_NAME = "read_timeout";
  /**
   * This constant will define write timeout key name.
   */
  public static final String WRITE_TIMEOUT_KEY_NAME = "write_timeout";
  /**
   * This constant will define api retry delay key name.
   */
  public static final String RETRY_DELAY_KEY_NAME = "retry_delay";
  /**
   * This field will store get accounts sub path.
   */
  public static final String GET_ACCOUNTS_SUB_PATH = "Accounts";
  /**
   * This constant will define accounts sorting field name.
   */
  public static final String SORT_ACCOUNTS_FIELD_NAME = "name";
  /**
   * This constant will define account entity key.
   */
  public static final String ACCOUNT_ENTITY_KEY = "account";
  /**
   * This constant will define directory service entity key.
   */
  public static final String DIRECTORY_SERVICES_KEY = "directoryservice";
  /**
   * This constant will define allowed primary key attributes.
   */
  public static final List<String> ALLOWED_PRIMARY_KEY_ATTRIBUTES = ImmutableList.of("name", "id");
  /**
   * This constant will define directory services endpoint.
   */
  public static final String DIRECTORY_SERVICES_ENDPOINT = "core/GetDirectoryServices";
  /**
   * Constant defining role rights endpoint.
   */
  public static final String ROLE_RIGHTS_ENDPOINT = "core/GetAssignedAdministrativeRights";
  /**
   * Constant to define start index value to get group name prefixed with (Group).
   */
  public static final int GROUP_START_INDEX = "(Group)".length();
  /**
   * Constant to define Row key name.
   */
  public static final String ROW_KEY_NAME = "Row";
  /**
   * Constant to define get roles payload json string when like command to be used for value
   * search.
   */
  public static final String GET_ROLES_PAYLOAD_FOR_FILTERS_WITH_LIKE_COMMAND =
      "{\n" + "    \"Script\": \"select * from Roles where LOWER(%s) LIKE '%s'\",\n" +
          "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Name\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * Constant to define get roles payload json string when assignment operator to be used for value
   * search.
   */
  public static final String GET_ROLES_PAYLOAD_FOR_FILTERS_WITH_EXACT_VALUE =
      "{\n" + "    \"Script\": \"select * from Roles where LOWER(%s)='%s'\",\n" +
          "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Name\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * Constant to define get users payload json string when like command to be used for value
   * search.
   */
  public static final String GET_USERS_PAYLOAD_FOR_FILTERS_FOR_LIKE_COMMAND =
      "{\n" + "    \"Script\": \"select * from User where LOWER(%s) LIKE '%s'\",\n" +
          "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Username\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * Constant to define get users payload json string when assignment operator to be used for value
   * search.
   */
  public static final String GET_USERS_PAYLOAD_FOR_FILTERS_FOR_EXACT_VALUE =
      "{\n" + "    \"Script\": \"select * from User where LOWER(%s)='%s'\",\n" +
          "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Username\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * Constant to define get users payload json string when like command to be used for numeric
   * column value search.
   */
  public static final String GET_USERS_PAYLOAD_FOR_FILTERS_WITH_NUMERIC_COLUMNS_FOR_LIKE_COMMAND =
      "{\n" + "    \"Script\": \"select * from User where CAST(%s as TEXT) LIKE '%s'\",\n" +
          "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Username\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * Constant to define get users payload json string when assignment operator to be used for
   * numeric column value search.
   */
  public static final String GET_USERS_PAYLOAD_FOR_FILTERS_WITH_NUMERIC_COLUMNS_FOR_EXACT_VALUE =
      "{\n" + "    \"Script\": \"select * from User where %s=%s\",\n" +
          "    \"Args\": {\n"
          + "        \"PageNumber\": %d,\n" + "        \"PageSize\": %d,\n"
          + "        \"Limit\": %d,\n" + "        \"SortBy\": \"Username\",\n"
          + "        \"Ascending\": true,\n" + "        \"Direction\": \"ASC\",\n"
          + "        \"Caching\": -2\n" + "    }\n" + "}";
  /**
   * Constant to define NOT key.
   */
  public static final String NOT_OPERATOR_KEY = "NOT";
  /**
   * Constant to define NULL key.
   */
  public static final String NULL_KEY = "null";
  /**
   * Constant to define asterisk key.
   */
  public static final String ASTERISK_KEY = "*";
  /**
   * Constant to define percentage key.
   */
  public static final String PERCENTAGE_KEY = "%";
  /**
   * Constant to define invalid schema name message prefix.
   */
  public static final String INVALID_SCHEMA_NAME_MESSAGE_PREFIX = "invalid schema name: %s";
  /**
   * Constant to define get safes query filter key.
   */
  public static final String SAFE_FILTER_QUERY_PARAMETER_KEY = "search";
  /**
   * Constant to define get accounts query filter key.
   */
  public static final String ACCOUNT_FILTER_QUERY_PARAMETER_KEY = "filter";
  /**
   * Constant to define user source serialized key.
   */
  public static final String USER_SOURCE_SERIALIZED_NAME = "sourcedslocalized";
  /**
   * Constant to define user-name serialized key.
   */
  public static final String USER_NAME_SERIALIZED_NAME = "username";
  /**
   * Constant to define user mail serialized key.
   */
  public static final String USER_MAIL_SERIALIZED_NAME = "email";
  /**
   * Constant to define user risk level rank serialized key.
   */
  public static final String RISK_LEVEL_RANK_KEY_NAME = "risklevelrank";
  /**
   * Constant to define get accounts by safe name filter value prefix.
   */
  public static final String SAFE_NAME_FILTER_PREFIX = "safeName eq ";
  /**
   * Constant to define max filter size.
   */
  public static final int MAX_FILTERS_SIZE = 1;
  /**
   * Constant to define max filter value size.
   */
  public static final int MAX_FILTER_VALUE_SIZE = 1;
  /**
   * Constant to define error message when subdomain is not available.
   */
  public static final String SUBDOMAIN_NOT_AVAILABLE_ERROR_MESSAGE =
      "Config parameter Subdomain is required to get safes & accounts data.";
  /**
   * Constant to store json response key 'members'.
   */
  public static final String MEMBERS_KEY = "members";
  /**
   * GSON constant with a custom JSON serializer for RecordRow class.
   */
  public static final Gson GOOGLE_JSON_FOR_SERIALIZATION = new GsonBuilder()
      .registerTypeAdapter(RecordRow.class, new RecordRowUpdateSerializer())
      .create();
}