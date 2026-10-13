package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.BASE_SEARCH_REQUEST_KEY;
import static com.radiantlogic.utility.Constants.BUSINESS_PROCESSES_REPORT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.DATA_FETCH_FAILED_MESSAGE;
import static com.radiantlogic.utility.Constants.DOMAINS_REPORT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.INTEGRATION_SYSTEM_USERS_REPORT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.INVALID_OBJECT_RECEIVED_FOR_DATA_FETCH_EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.JOB_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.JOB_POSTING_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SECURITY_GROUPS_MEMBERSHIPS_REPORT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SECURITY_GROUPS_REPORT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SUPERVISORY_ORGANIZATION_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.WORKER_ENTITY_KEY;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.radiantlogic.enums.BusinessProcessAttribute;
import com.radiantlogic.enums.DomainAttribute;
import com.radiantlogic.enums.IntegrationSystemUserAttribute;
import com.radiantlogic.enums.SecurityGroupAttribute;
import com.radiantlogic.enums.SecurityGroupMembershipAttribute;
import com.radiantlogic.exception.WorkdayException;
import com.radiantlogic.factory.SchemaObjectDataProviderFactory;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.interfaces.SchemaObjectDataProvider;
import com.radiantlogic.model.BusinessProcess;
import com.radiantlogic.model.Domain;
import com.radiantlogic.model.IntegrationSystemUser;
import com.radiantlogic.model.ReportResponse;
import com.radiantlogic.model.SecurityGroup;
import com.radiantlogic.model.SecurityGroupMembership;
import com.radiantlogic.report.GenericReportDataExtractor;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import com.radiantlogic.utility.Util;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides paginated access to LDAP responses.
 *
 * <p>Implements a sequential data provider that retrieves results
 * in pages to support large dataset processing.</p>
 */
@Slf4j
@SuppressWarnings("PMD.ExcessiveImports")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification =
        "RequestBuilder is intentionally shared and not defensively copied.")
public final class PagedResponse implements SequentialDataProvider<LdapResponse<?>> {

  /**
   * Indicates whether this request has been invalidated and should no longer be processed.
   */
  private boolean invalidated;

  /**
   * Unique identifier used when fetching a single record.
   */
  private final String uniqueKey;

  /**
   * Schema configuration describing the target objects and their attributes.
   */
  private final ReadOnlyProperties targetSchemaObjects;

  /**
   * Schema definition associated with the requested data.
   */
  private final Schema schema;

  /**
   * Request context used for retrieving data for various objects.
   */
  private final GetRequest getRequest;

  /**
   * Builder used to construct requests and perform credential validation.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Name of the schema object being processed.
   */
  private final String schemaObjectName;

  /**
   * Indicates whether additional records are available for pagination.
   */
  private boolean hasMoreRecords = true;
  /**
   * Number of records to fetch per page in paginated API responses.
   */
  private final int pageSize;

  /**
   * Converter used to transform search responses into internal data models.
   */
  private static final SearchResponseConverter SEARCH_RESPONSE_CONVERTER =
      new SearchResponseConverter();

  /**
   * Data provider responsible for fetching schema object data at runtime.
   */
  private SchemaObjectDataProvider schemaObjectDataProvider;

  /**
   * Registry mapping schema object identifiers to their corresponding data provider factories.
   */
  private static final Map<String, Function<PagedResponse, SchemaObjectDataProvider>>
      HANDLER_REGISTRY = createRegistry();

  /**
   * Creates a new PagedResponse with the provided request and schema context.
   * Use {@link #builder()} to create instances.
   *
   * @param searchRequest        the LDAP search request containing query and filter details
   * @param targetSchemaObjects  the schema configuration for the requested objects
   * @param schema               the schema definition associated with the requested data
   * @param getRequest           the request context used for retrieving object data
   * @param requestBuilder       builds requests and performs credential validation
   * @param pageSize             set the page size to limit number for records per page
   */
  @Builder
  private PagedResponse(
      final LdapSearchRequest searchRequest,
      final ReadOnlyProperties targetSchemaObjects,
      final Schema schema,
      final GetRequest getRequest,
      final RequestBuilder requestBuilder,
      final int pageSize) {
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
    this.pageSize = pageSize;
    determineWhichObjectRequest();
  }

  private static Map<String, Function<PagedResponse, SchemaObjectDataProvider>> createRegistry() {
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    Map<String, Function<PagedResponse, SchemaObjectDataProvider>> map =
        new HashMap<>();

    map.put(WORKER_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createWorkersProvider(
            pagedResponse.getRequest, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER));
    map.put(JOB_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createJobsProvider(
            pagedResponse.getRequest, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER));
    map.put(JOB_POSTING_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createJobPostingProvider(
            pagedResponse.getRequest, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER));
    map.put(SUPERVISORY_ORGANIZATION_ENTITY_KEY,
        pagedResponse -> SchemaObjectDataProviderFactory.createSupervisoryOrganizationProvider(
            pagedResponse.getRequest, pagedResponse.uniqueKey,
            pagedResponse.schema, pagedResponse.targetSchemaObjects, SEARCH_RESPONSE_CONVERTER));
    map.put(INTEGRATION_SYSTEM_USERS_REPORT_ENTITY_KEY,
        pagedResponse -> {
          String reportUrl = pagedResponse.requestBuilder.getIntegrationSystemUsersReportUrl();
          return SchemaObjectDataProviderFactory.createReportProvider(
              pagedResponse.getRequest, pagedResponse.uniqueKey, reportUrl,
              pagedResponse.schema, pagedResponse.targetSchemaObjects,
              SEARCH_RESPONSE_CONVERTER,
              new TypeToken<ReportResponse<IntegrationSystemUser>>() {
              }.getType(),
              GenericReportDataExtractor.of(
                  IntegrationSystemUserAttribute::fromName,
                  IntegrationSystemUser::getUserName));
        });
    map.put(SECURITY_GROUPS_REPORT_ENTITY_KEY,
        pagedResponse -> {
          String reportUrl = pagedResponse.requestBuilder.getSecurityGroupsReportUrl();
          return SchemaObjectDataProviderFactory.createReportProvider(
              pagedResponse.getRequest, pagedResponse.uniqueKey, reportUrl,
              pagedResponse.schema, pagedResponse.targetSchemaObjects,
              SEARCH_RESPONSE_CONVERTER,
              new TypeToken<ReportResponse<SecurityGroup>>() {
              }.getType(),
              GenericReportDataExtractor.of(
                  SecurityGroupAttribute::fromName,
                  SecurityGroup::getWorkdayId));
        });
    map.put(SECURITY_GROUPS_MEMBERSHIPS_REPORT_ENTITY_KEY,
        pagedResponse -> {
          String reportUrl = pagedResponse.requestBuilder.getSecurityGroupsMembershipsReportUrl();
          return SchemaObjectDataProviderFactory.createReportProvider(
              pagedResponse.getRequest, pagedResponse.uniqueKey, reportUrl,
              pagedResponse.schema, pagedResponse.targetSchemaObjects,
              SEARCH_RESPONSE_CONVERTER,
              new TypeToken<ReportResponse<SecurityGroupMembership>>() {
              }.getType(),
              GenericReportDataExtractor.of(
                  SecurityGroupMembershipAttribute::fromName,
                  SecurityGroupMembership::getWorkdayId));
        });
    map.put(DOMAINS_REPORT_ENTITY_KEY,
        pagedResponse -> {
          String reportUrl = pagedResponse.requestBuilder.getDomainsReportUrl();
          return SchemaObjectDataProviderFactory.createReportProvider(
              pagedResponse.getRequest, pagedResponse.uniqueKey, reportUrl,
              pagedResponse.schema, pagedResponse.targetSchemaObjects,
              SEARCH_RESPONSE_CONVERTER,
              new TypeToken<ReportResponse<Domain>>() {
              }.getType(),
              GenericReportDataExtractor.of(
                  DomainAttribute::fromName,
                  Domain::getName));
        });
    map.put(BUSINESS_PROCESSES_REPORT_ENTITY_KEY,
        pagedResponse -> {
          String reportUrl = pagedResponse.requestBuilder.getBusinessProcessesReportUrl();
          return SchemaObjectDataProviderFactory.createReportProvider(
              pagedResponse.getRequest, pagedResponse.uniqueKey, reportUrl,
              pagedResponse.schema, pagedResponse.targetSchemaObjects,
              SEARCH_RESPONSE_CONVERTER,
              new TypeToken<ReportResponse<BusinessProcess>>() {
              }.getType(),
              GenericReportDataExtractor.of(
                  BusinessProcessAttribute::fromName,
                  BusinessProcess::getName));
        });
    return Collections.unmodifiableMap(map);
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

  @Override
  public LdapResponse<String> next() {
    try {
      List<SearchResultEntry> searchResultEntries = getNextPage();
      return new LdapResponse<>(LdapResultCode.SUCCESS,
          SEARCH_RESPONSE_CONVERTER.toJson(searchResultEntries));
    } catch (JsonParseException | WorkdayException | IOException e) {
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
      return false;
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

  private List<SearchResultEntry> getNextPage()
      throws IOException, WorkdayException {
    if (!isValid()) {
      throw new WorkdayException(DATA_FETCH_FAILED_MESSAGE);
    }
    List<SearchResultEntry> searchResultEntries = schemaObjectDataProvider.getNextPage(pageSize);
    this.hasMoreRecords = schemaObjectDataProvider.hasMoreRecords();
    return searchResultEntries;
  }

}
