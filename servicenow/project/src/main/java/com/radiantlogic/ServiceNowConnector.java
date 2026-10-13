package com.radiantlogic;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;

import com.radiantlogic.create.CreateUserRequest;
import com.radiantlogic.delete.DeleteRequest;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.PagedResponse;
import com.radiantlogic.http.HttpClient;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.annotations.CustomConnector;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.CreateOperations;
import com.radiantlogic.iddm.base.DeleteOperations;
import com.radiantlogic.iddm.base.InjectableProperties;
import com.radiantlogic.iddm.base.ModifyOperations;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SearchOperations;
import com.radiantlogic.iddm.base.SearchResponse;
import com.radiantlogic.iddm.base.TestConnectionOperations;
import com.radiantlogic.iddm.base.TestConnectionRequest;
import com.radiantlogic.iddm.base.TestConnectionResponse;
import com.radiantlogic.iddm.base.Uri;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.update.UpdateRequestDispatcher;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Class which will serve as entry point for the connector.
 */
@Slf4j
@CustomConnector(configuration = "servicenow.json")
@Getter
@SuppressWarnings("PMD.ExcessiveImports")
public final class ServiceNowConnector
    implements TestConnectionOperations<TestConnectionRequest, TestConnectionResponse>,
    SearchOperations<LdapSearchRequest, SearchResponse>,
    DeleteOperations<LdapDeleteRequest, LdapResponse<String>>,
    ModifyOperations<LdapModifyRequest, LdapResponse<Void>>,
    CreateOperations<LdapAddRequest, LdapResponse<Void>> {

  /**
   * Field containing target schema objects.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * Field to fetch schema details corresponding to request made.
   */
  private final Schema schema;
  /**
   * Field containing connection properties.
   */
  private final ReadOnlyProperties connectionProperties;
  /**
   * Field to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Field used to execute requests.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Field used to get data for various objects.
   */
  private final GetRequest getRequest;
  /**
   * Field to call various delete object operations.
   */
  private final DeleteRequest deleteRequest;
  /**
   * Field to call various update objects operations.
   */
  private final UpdateRequestDispatcher updateRequestDispatcher;
  /**
   * Field to handle create user request.
   */
  private final CreateUserRequest createUserRequest;

  /**
   * Constructor to initialize instance members.
   *
   * @param targetSchemaObjects  ReadOnlyProperties for request
   * @param schema               Schema for request
   * @param connectionProperties ReadOnlyProperties - Config properties
   */
  public ServiceNowConnector(@Property(name = InjectableProperties.TARGET_SCHEMA_OBJECTS)
                             final ReadOnlyProperties targetSchemaObjects,
                             @Property(name = InjectableProperties.SCHEMAS) final Schema schema,
                             @Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                             final ReadOnlyProperties connectionProperties,
                             final @NonNull HttpClient httpClient) {

    log.info("***** Instantiating ServiceNow Connector *****");

    this.targetSchemaObjects = targetSchemaObjects;
    this.connectionProperties = connectionProperties;
    this.schema = schema;
    requestExecutor = new RequestExecutor(httpClient);
    this.requestBuilder = new RequestBuilder(connectionProperties, requestExecutor);
    this.getRequest =
        new GetRequest(requestBuilder, requestExecutor, this.requestBuilder.getBatchSize());
    this.deleteRequest = new DeleteRequest(requestBuilder, requestExecutor, getRequest);
    updateRequestDispatcher =
        new UpdateRequestDispatcher(requestBuilder, requestExecutor, getRequest,
            targetSchemaObjects, schema, this.requestBuilder.getBatchSize());
    createUserRequest =
        new CreateUserRequest(requestBuilder, requestExecutor, targetSchemaObjects, schema);
  }

  @Override
  public TestConnectionResponse testConnection(final TestConnectionRequest testConnectionRequest) {
    return new TestConnectionResponse(
        Uri.fromString(requestBuilder.getBaseUrl().build().toString()),
        requestBuilder.canConnectWithValidToken());
  }

  @Override
  public SearchResponse search(final LdapSearchRequest searchRequest) {
    return new PagedResponse(searchRequest, targetSchemaObjects, schema, getRequest,
        requestBuilder);
  }

  @Override
  public LdapResponse<String> delete(final LdapDeleteRequest ldapDeleteRequest) {
    return deleteRequest.delete(ldapDeleteRequest);
  }

  @Override
  public LdapResponse<Void> modify(final @NonNull LdapModifyRequest ldapModifyRequest) {
    return updateRequestDispatcher.modify(ldapModifyRequest);
  }

  @Override
  public LdapResponse<Void> create(final @NonNull LdapAddRequest ldapAddRequest) {
    return createUserRequest.create(ldapAddRequest);
  }
}
