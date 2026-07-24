package com.radiantlogic;

import static com.radiantlogic.iddm.base.InjectableProperties.CUSTOM_DATASOURCE_PROPERTIES;
import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.radiantlogic.create.CreateRequest;
import com.radiantlogic.delete.DeleteRequest;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.get.PagedResponse;
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
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.update.UpdateRequest;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This class will serve as entry point for the connector.
 */
@Slf4j
@CustomConnector(configuration = "cyberark_privilege_cloud.json")
@Getter
@SuppressWarnings(PMD_KEY)
public class CyberArkConnector implements SearchOperations<LdapSearchRequest, SearchResponse>,
    TestConnectionOperations<TestConnectionRequest, TestConnectionResponse>,
    DeleteOperations<LdapDeleteRequest, LdapResponse<String>>,
    ModifyOperations<LdapModifyRequest, LdapResponse<Void>>,
    CreateOperations<LdapAddRequest, LdapResponse<Void>> {

  /**
   * This object will be used to read target schema objects.
   */

  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * This object will be used to fetch schema details corresponding to request made.
   */
  private final Schema schema;
  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * This object will be used to get data for various objects.
   */

  private final GetRequest getRequest;
  /**
   * This object will be used to call various update object operations.
   */
  private final UpdateRequest updateRequest;
  /**
   * This object will be used to call various create object operations.
   */
  private final CreateRequest createRequest;
  /**
   * This object will be used to call various delete object operations.
   */
  private final DeleteRequest deleteRequest;
  /**
   * This object will be used to get url & payload for various create objects operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * This object will contain connection properties.
   */
  private final ReadOnlyProperties connectionProperties;

  /**
   * This constructor will initialize class's instance members.
   *
   * @param targetSchemaObjects  ReadOnlyProperties for request
   * @param schema               Schema for request
   * @param connectionProperties ReadOnlyProperties
   */
  public CyberArkConnector(@Property(name = InjectableProperties.TARGET_SCHEMA_OBJECTS)
                           final ReadOnlyProperties targetSchemaObjects,
                           @Property(name = InjectableProperties.SCHEMAS) final Schema schema,
                           @Property(name = CUSTOM_DATASOURCE_PROPERTIES)
                           final ReadOnlyProperties connectionProperties) {
    this.targetSchemaObjects = targetSchemaObjects;
    this.connectionProperties = connectionProperties;
    this.schema = schema;
    requestBuilder = new RequestBuilder(connectionProperties);
    requestExecutor = new RequestExecutor(connectionProperties, requestBuilder);
    // instantiated using new keyword as we need token (contained in Executor class to be shared
    // by all)
    getRequest = new GetRequest(requestBuilder, requestExecutor);
    createRequest =
        new CreateRequest(requestBuilder, getRequest, requestExecutor, targetSchemaObjects, schema);
    updateRequest =
        new UpdateRequest(requestBuilder, getRequest, requestExecutor, targetSchemaObjects, schema);
    deleteRequest = new DeleteRequest(requestBuilder, getRequest, requestExecutor);
  }

  @Override
  public TestConnectionResponse testConnection(final TestConnectionRequest testConnectionRequest) {

    if (!testConnectionRequest.getTarget().equals(Uri.EMPTY)) {
      log.error("Test connection request includes an empty target URI");
      return new TestConnectionResponse(testConnectionRequest.getTarget(), false);
    }
    return new TestConnectionResponse(
        Uri.fromString(requestBuilder.getBaseUrl().build().toString()),
        requestExecutor.canConnect());
  }

  @Override
  public SearchResponse search(final LdapSearchRequest searchRequest) {
    return new PagedResponse(searchRequest, targetSchemaObjects, schema, getRequest,
        requestExecutor);
  }

  @Override
  public LdapResponse<String> delete(final LdapDeleteRequest ldapDeleteRequest) {
    return deleteRequest.delete(ldapDeleteRequest);
  }

  @Override
  public LdapResponse<Void> modify(final @NonNull LdapModifyRequest ldapModifyRequest) {
    return updateRequest.modify(ldapModifyRequest);
  }

  @Override
  public LdapResponse<Void> create(final @NonNull LdapAddRequest ldapAddRequest) {
    return createRequest.create(ldapAddRequest);
  }
}
