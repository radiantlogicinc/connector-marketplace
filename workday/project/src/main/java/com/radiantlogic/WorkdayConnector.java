package com.radiantlogic;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;

import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.PagedResponse;
import com.radiantlogic.http.HttpClient;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.annotations.CustomConnector;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.InjectableProperties;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchOperations;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.base.TestConnectionOperations;
import com.radiantlogic.iddm.base.TestConnectionRequest;
import com.radiantlogic.iddm.base.TestConnectionResponse;
import com.radiantlogic.iddm.base.Uri;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * Main connector implementation for integrating with Workday services.
 *
 * <p>This class is responsible for handling connector lifecycle operations such as
 * establishing and validating connections, executing requests, and interacting with Workday APIs
 * based on the provided configuration. Provides access to Workers, Jobs, Postings and Supervisory
 * Organizations data.
 */
@Slf4j
@CustomConnector(configuration = "workday.json")
@Getter
public final class WorkdayConnector
    implements TestConnectionOperations<TestConnectionRequest, TestConnectionResponse>,
    SearchOperations<LdapSearchRequest, SequentialDataProvider<LdapResponse<?>>> {

  /**
   * Read-only configuration defining target schema objects to be processed.
   */
  private final ReadOnlyProperties targetSchemaObjects;

  /**
   * Schema definition used to interpret and map incoming requests and responses.
   */
  private final Schema schema;

  /**
   * Read-only connection configuration properties used to establish and manage connectivity with
   * the external system.
   */
  private final ReadOnlyProperties connectionProperties;

  /**
   * Builder responsible for constructing requests and managing authentication-related operations.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Executor responsible for performing HTTP request execution and returning responses.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Request context used for retrieving data for various objects.
   */
  private final GetRequest getRequest;

  /**
   * Constructs a WorkdayConnector with the required configuration, schema, and HTTP client.
   *
   * <p>This constructor initializes the connector by setting up schema metadata,
   * connection properties, and building the request execution pipeline. It also creates internal
   * components such as {@link RequestExecutor} and {@link RequestBuilder} to handle request
   * execution and authentication.
   *
   * @param targetSchemaObjects  contains metadata for supported target schema objects
   * @param schema               schema definition used for request mapping
   * @param connectionProperties connection configuration properties for Workday integration
   * @param httpClient           HTTP client used to execute external API requests
   */
  public WorkdayConnector(@Property(name = InjectableProperties.TARGET_SCHEMA_OBJECTS)
                          final ReadOnlyProperties targetSchemaObjects,
                          @Property(name = InjectableProperties.SCHEMAS) final Schema schema,
                          @Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                          final ReadOnlyProperties connectionProperties,
                          final HttpClient httpClient) {
    this.targetSchemaObjects = targetSchemaObjects;
    this.connectionProperties = connectionProperties;
    this.schema = schema;
    this.requestExecutor = new RequestExecutor(httpClient);
    this.requestBuilder = new RequestBuilder(connectionProperties, requestExecutor);
    this.getRequest =
        new GetRequest(requestBuilder, requestExecutor);
  }

  @Override
  public TestConnectionResponse testConnection(final TestConnectionRequest testConnectionRequest) {
    return new TestConnectionResponse(
        Uri.fromString(requestBuilder.getBaseUrl().build().toString()),
        requestBuilder.canEstablishConnection());
  }

  @Override
  public SequentialDataProvider<LdapResponse<?>> search(final LdapSearchRequest searchRequest) {
    return PagedResponse.builder()
        .searchRequest(searchRequest)
        .targetSchemaObjects(targetSchemaObjects)
        .schema(schema)
        .getRequest(getRequest)
        .requestBuilder(requestBuilder)
        .pageSize(requestBuilder.getPageSize())
        .build();
  }
}
