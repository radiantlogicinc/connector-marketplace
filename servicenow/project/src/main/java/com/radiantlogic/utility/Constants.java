package com.radiantlogic.utility;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.radiantlogic.model.ServiceNowResponse;
import java.util.List;
import okhttp3.MediaType;

/**
 * Class to define constants used by the connector.
 */
@SuppressWarnings("PMD.DataClass")
public final class Constants {

  /**
   * Constant to define URL scheme value.
   */
  public static final String URL_SCHEME = "https";
  /**
   * Constant to define grant type key value.
   */
  public static final String GRANT_TYPE_KEY = "grant_type";
  /**
   * Constant to define client credentials key value.
   */
  public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
  /**
   * Constant to define instance name key value.
   */
  public static final String INSTANCE_NAME_KEY = "instance_name";
  /**
   * Constant to define client id key value.
   */
  public static final String CLIENT_ID_KEY = "client_id";
  /**
   * Constant to define client secret key value.
   */
  public static final String CLIENT_SECRET_KEY = "client_secret";
  /**
   * Constant to define page size key value.
   */
  public static final String PAGE_SIZE_KEY = "page_size";
  /**
   * Constant to define missing required connection property prefix message value.
   */
  public static final String MISSING_REQUIRED_CONNECTION_PROPERTY_PREFIX =
      "Missing required connection property: %s";
  /**
   * Constant to define exception prefix value.
   */
  public static final String EXCEPTION_PREFIX = "Exception is: ";
  /**
   * Constant to define failed to get data message value.
   */
  public static final String DATA_FETCH_FAILED_MESSAGE = "Failed to get the data";
  /**
   * Constant to define bearer key with space value.
   */
  public static final String BEARER_KEYWORD_WITH_SPACE = "Bearer ";
  /**
   * Constant to define seconds to subtract while calculating token expiry.
   */
  public static final int TOKEN_EXPIRY_BUFFER_SECONDS = 120;
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
   * Constant to define URL scheme key name.
   */
  public static final String URL_SCHEME_KEY = "scheme";
  /**
   * Constant to define URL host key name.
   */
  public static final String URL_HOST_KEY = "host";
  /**
   * Constant to define URL port key name.
   */
  public static final String URL_PORT_KEY = "port";
  /**
   * Constant to define base url prefix value.
   */
  public static final String BASE_URL_PREFIX = "%s.service-now.com";
  /**
   * Constant to define get access token endpoint value.
   */
  public static final String GENERATE_ACCESS_TOKEN_ENDPOINT = "oauth_token.do";
  /**
   * Constant to define connection timeout key name.
   */
  public static final String CONNECTION_TIMEOUT_KEY_NAME = "connect_timeout";
  /**
   * Constant to define read timeout key name.
   */
  public static final String READ_TIMEOUT_KEY_NAME = "read_timeout";
  /**
   * Constant to define write timeout key name.
   */
  public static final String WRITE_TIMEOUT_KEY_NAME = "write_timeout";
  /**
   * Constant to define Google Gson.
   */
  public static final Gson GSON = new Gson();
  /**
   * Constant to define api retry delay key name.
   */
  public static final String RETRY_DELAY_KEY_NAME = "retry_delay";
  /**
   * Constant to define authorization key value.
   */
  public static final String AUTHORIZATION_KEYWORD = "Authorization";
  /**
   * Constant to define unable to connect with servicenow message value.
   */
  public static final String UNABLE_TO_CONNECT_WITH_SERVICENOW_MESSAGE =
      "Unable to connect with ServiceNow, please check the credentials";
  /**
   * Constant to define allowed primary key attributes.
   */
  public static final List<String> ALLOWED_PRIMARY_KEY_ATTRIBUTES =
      ImmutableList.of("username", "name");
  /**
   * Constant to define schema objects key value.
   */
  public static final String SCHEMA_OBJECTS_KEY = "schemaObjects";
  /**
   * Constant to define size one value.
   */
  public static final int SIZE_ONE = 1;
  /**
   * Constant to define base search key value.
   */
  public static final String BASE_SEARCH_REQUEST_KEY = "BASE";
  /**
   * Constant to define user entity value.
   */
  public static final String USER_ENTITY_KEY = "user";
  /**
   * Constant to define group entity value.
   */
  public static final String GROUP_ENTITY_KEY = "group";
  /**
   * Constant to define role entity value.
   */
  public static final String ROLE_ENTITY_KEY = "role";
  /**
   * Constant to define api response key - result.
   */
  public static final String RESULT_KEY = "result";
  /**
   * Constant to define link response header key.
   */
  public static final String LINK_HEADER_KEY = "Link";
  /**
   * Constant to define invalid object received for data fetch exception prefix.
   */
  public static final String INVALID_OBJECT_RECEIVED_FOR_DATA_FETCH_EXCEPTION_PREFIX =
      "Invalid object received for data fetch: %s";
  /**
   * Constant to define could not find schema object exception prefix.
   */
  public static final String COULD_NOT_FIND_SCHEMA_OBJECT_EXCEPTION_PREFIX =
      "Could not find schema object: ";
  /**
   * Constant to define expected single primary key exception prefix.
   */
  public static final String EXPECTED_SINGLE_PRIMARY_KEY_EXCEPTION_MESSAGE =
      "expected single primary key attribute";
  /**
   * Constant to define missing required primary key exception prefix.
   */
  public static final String MISSING_REQUIRED_PRIMARY_KEY_ATTRIBUTE_EXCEPTION_MESSAGE =
      "missing required primary key attribute name";
  /**
   * Constant to define unable to get schema object name exception prefix.
   */
  public static final String UNABLE_TO_GET_SCHEMA_OBJECT_NAME_EXCEPTION_MESSAGE =
      "Unable to get schema object type for current search/Expecting IDDM to only ever send one "
          + "schema object per LDAP request";
  /**
   * Constant to define custom JSON deserializer for parsing servicenow response.
   */
  public static final Gson SERVICE_NOW_RESPONSE_GSON = new GsonBuilder()
      .registerTypeAdapter(ServiceNowResponse.class, new ServiceNowResponseDeserializer<>())
      .create();
  /**
   * Constant to define user endpoint sub path.
   */
  public static final String USERS_ENDPOINT_SUB_PATH = "api/now/table/sys_user";
  /**
   * Constant to define group endpoint sub path.
   */
  public static final String GROUPS_ENDPOINT_SUB_PATH = "api/now/table/sys_user_group";
  /**
   * Constant to define role endpoint sub path.
   */
  public static final String ROLES_ENDPOINT_SUB_PATH = "api/now/table/sys_user_role";
  /**
   * Constant to define child roles endpoint sub path.
   */
  public static final String CHILD_ROLES_ENDPOINT_SUB_PATH = "api/now/table/sys_user_role_contains";
  /**
   * Constant to define user roles endpoint sub path.
   */
  public static final String USER_ROLES_ENDPOINT_SUB_PATH = "api/now/table/sys_user_has_role";
  /**
   * Constant to define group users endpoint sub path.
   */
  public static final String GROUP_USERS_ENDPOINT_SUB_PATH = "api/now/table/sys_user_grmember";
  /**
   * Constant to define group roles endpoint sub path.
   */
  public static final String GROUP_ROLES_ENDPOINT_SUB_PATH = "api/now/table/sys_group_has_role";
  /**
   * Constant to define page size query parameter key.
   */
  public static final String PAGE_SIZE_QUERY_PARAMETER = "sysparm_limit";
  /**
   * Constant to define filter query parameter key.
   */
  public static final String FILTER_QUERY_PARAMETER = "sysparm_query";

  /**
   * Constant to define entities on which various CUD operations can be performed.
   */
  public static final List<String> ALLOWED_ENTITIES = ImmutableList.of("user", "group", "role");
  /**
   * Constant to define attribute details log message value.
   */
  public static final String ATTRIBUTE_NAME_WITH_VALUES_AND_MODIFICATION_TYPE_LOG_MESSAGE =
      "attribute name is :{} with values:{} and modification type is:{}";
  /**
   * Constant to define JSON media type.
   */
  public static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json; charset=utf-8");
  /**
   * Constant to define allowed boolean values as string.
   */
  public static final List<String> BOOLEAN_ALLOWED_VALUES = ImmutableList.of("true", "false");
  /**
   * Constant to define name attribute value change error.
   */
  public static final String NAME_ATTRIBUTE_VALUE_CHANGE_ERROR =
      "name attribute cannot be set/unset, it will be automatically set based upon first "
          + "name and last name values";
  /**
   * Constant to define sys param fields query parameter key name.
   */
  public static final String SYS_PARAM_FIELDS_KEY = "sysparm_fields";
  /**
   * Constant to define sys_id field name.
   */
  public static final String SYS_ID_FIELD = "sys_id";
  /**
   * Encoded query token used in ServiceNow sysparm_query strings to join values with a logical OR
   * operator.
   */
  public static final String OR_SEPARATOR = "^OR";
  /**
   * Constant to define batch size key value.
   */
  public static final String BATCH_SIZE_KEY = "batch_size";
  /**
   * Filter field prefix for child roles queries.
   */
  public static final String CHILD_ROLES_FILTER_FIELD = "role.name=";
  /**
   * Sysparm fields value for child roles queries.
   */
  public static final String CHILD_ROLES_SYSPARM_FIELDS = "role.name,contains.name";
  /**
   * Filter field prefix for group users queries.
   */
  public static final String GROUP_USERS_FILTER_FIELD = "group.name=";
  /**
   * Sysparm fields value for group users queries.
   */
  public static final String GROUP_USERS_SYSPARM_FIELDS = "user.user_name,group.name";
  /**
   * Filter field prefix for user roles queries.
   */
  public static final String USER_ROLES_FILTER_FIELD = "user.user_name=";
  /**
   * Sysparm fields value for user roles queries.
   */
  public static final String USER_ROLES_SYSPARM_FIELDS = "role.name,user.user_name";
  /**
   * Filter field prefix for group roles queries.
   */
  public static final String GROUP_ROLES_FILTER_FIELD = "group.name=";
  /**
   * Sysparm fields value for group roles queries.
   */
  public static final String GROUP_ROLES_SYSPARM_FIELDS = "role.name,group.name";
  /**
   * Query parameter 'inherited' key name.
   */
  public static final String INHERITED_QUERY_PARAMETER_KEY = "inherited";
  /**
   * Query parameter value false.
   */
  public static final String FALSE_QUERY_PARAMETER_VALUE = "false";

  private Constants() {
  }
}
