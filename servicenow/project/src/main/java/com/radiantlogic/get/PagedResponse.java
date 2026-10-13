package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.BASE_SEARCH_REQUEST_KEY;
import static com.radiantlogic.utility.Constants.DATA_FETCH_FAILED_MESSAGE;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.GROUP_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.INVALID_OBJECT_RECEIVED_FOR_DATA_FETCH_EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.ROLE_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.USER_ENTITY_KEY;

import com.google.gson.JsonParseException;
import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.factory.SchemaObjectDataProviderFactory;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.utility.GroupUtil;
import com.radiantlogic.utility.RoleUtil;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import com.radiantlogic.utility.UserUtil;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to fetch paginated data for various objects.
 */
@Slf4j
@SuppressWarnings("PMD.ExcessiveImports")
public class PagedResponse implements SequentialDataProvider<LdapResponse<?>> {

  /**
   * Field to store whether request has been invalidated.
   */
  private boolean invalidated;
  /**
   * Field to store unique key in case of fetching a single record.
   */
  private final String uniqueKey;
  /**
   * Field to store request object's details.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * Field to store requested object's schema.
   */
  private final Schema schema;
  /**
   * Field to get data for various objects.
   */
  private final GetRequest getRequest;
  /**
   * Field to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Field to store schema object name.
   */
  private final String schemaObjectName;
  /**
   * Field to store next link for pagination.
   */
  private String nextLink;
  /**
   * Field to store whether more records are there in case of pagination.
   */
  private boolean hasMoreRecords = true;
  /**
   * Field used to convert data.
   */
  private static final SearchResponseConverter SEARCH_RESPONSE_CONVERTER =
      new SearchResponseConverter();
  /**
   * Field used to store handler for object for which data to be fetched at runtime.
   */
  private SchemaObjectDataProvider schemaObjectDataProvider;
  /**
   * Field used to store registry mapping schema object identifiers to their corresponding data
   * provider factory functions.
   */
  private static final Map<String, Function<PagedResponse, SchemaObjectDataProvider>>
      HANDLER_REGISTRY = createRegistry();

  /**
   * Constructor to initialize instance members.
   *
   * @param searchRequest       Field containing search request details
   * @param targetSchemaObjects Field containing requested object schema data
   * @param schema              Field containing requested object schema
   * @param getRequest          Field used to make get requests to various objects
   * @param requestBuilder      Field to get request object for various objects & also credentials
   *                            checker.
   */
  public PagedResponse(
      final LdapSearchRequest searchRequest,
      final ReadOnlyProperties targetSchemaObjects,
      final Schema schema,
      final GetRequest getRequest,
      final RequestBuilder requestBuilder) {
    this.targetSchemaObjects = targetSchemaObjects;
    this.schema = schema;
    this.getRequest = getRequest;
    this.requestBuilder = requestBuilder;
    this.schemaObjectName = Util.getSchemaObjectName(targetSchemaObjects);
    Util.checkWhetherPrimaryKeyAttributeIsCorrectlySet(
        Util.getSchemaObject(schema, schemaObjectName));
    this.uniqueKey =
        BASE_SEARCH_REQUEST_KEY.equalsIgnoreCase(searchRequest.getSearchScope().name())
            ? Util.getUniqueKey(searchRequest) : "";
    determineWhichObjectRequest();
  }

  private static Map<String, Function<PagedResponse, SchemaObjectDataProvider>> createRegistry() {
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    Map<String, Function<PagedResponse, SchemaObjectDataProvider>> map =
        new HashMap<>();

    map.put(USER_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createUsersProvider(
            pagedResponse.getRequest, pagedResponse.nextLink, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER,
            UserUtil.isGetUserRoles(pagedResponse.schema, pagedResponse.targetSchemaObjects)));
    map.put(GROUP_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createGroupsProvider(
            pagedResponse.getRequest, pagedResponse.nextLink, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER,
            GroupUtil.isGetGroupUsers(pagedResponse.schema, pagedResponse.targetSchemaObjects),
            GroupUtil.isGetGroupRoles(pagedResponse.schema, pagedResponse.targetSchemaObjects)));
    map.put(ROLE_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createRolesProvider(
            pagedResponse.getRequest, pagedResponse.nextLink, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER,
            RoleUtil.checkWhetherFetchChildRoles(pagedResponse.schema,
                pagedResponse.schemaObjectName)));
    return Collections.unmodifiableMap(map);
  }

  @Override
  public LdapResponse<String> next() {
    try {
      List<SearchResultEntry> searchResultEntries = getNextPage();
      return new LdapResponse<>(LdapResultCode.SUCCESS,
          SEARCH_RESPONSE_CONVERTER.toJson(searchResultEntries));
    } catch (JsonParseException | ServiceNowException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
      invalidate();
      return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
    }
  }

  @Override
  public boolean hasMore() {
    if (!isValid()) {
      String errorMessage =
          "Unable to check for more data because the provider is in an invalid state.";
      log.error(errorMessage);
      throw new IllegalStateException(errorMessage);
    }
    return hasMoreRecords;
  }

  @Override
  public boolean isValid() {
    return !invalidated && requestBuilder.canConnectWithValidToken();
  }

  @Override
  public void invalidate() {
    invalidated = true;
  }

  private void determineWhichObjectRequest() {
    String normalizedSchemaObjectName = schemaObjectName.trim().toLowerCase(Locale.ROOT);
    log.debug("Available schema object handlers: {}", HANDLER_REGISTRY.keySet());
    schemaObjectDataProvider =
        Optional.ofNullable(HANDLER_REGISTRY.get(normalizedSchemaObjectName))
            .map(factory -> factory.apply(this))
            .orElseThrow(() -> {
              String errorMessage =
                  String.format(INVALID_OBJECT_RECEIVED_FOR_DATA_FETCH_EXCEPTION_PREFIX,
                      normalizedSchemaObjectName);
              log.error(errorMessage);
              return new IllegalStateException(errorMessage);
            });
  }

  private List<SearchResultEntry> getNextPage()
      throws IOException, ServiceNowException {
    if (!isValid()) {
      throw new ServiceNowException(DATA_FETCH_FAILED_MESSAGE);
    }
    List<SearchResultEntry> searchResultEntries = schemaObjectDataProvider.getNextPage();
    this.nextLink = schemaObjectDataProvider.getNextLink();
    this.hasMoreRecords = schemaObjectDataProvider.hasMoreRecords();
    return searchResultEntries;
  }
}
