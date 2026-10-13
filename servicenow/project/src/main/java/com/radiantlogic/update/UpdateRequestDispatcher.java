package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.interfaces.UpdateHandler;
import com.radiantlogic.model.RequestDetails;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispatcher responsible for processing update requests.
 *
 * <p>This class determines the appropriate {@link UpdateHandler} implementation based on
 * the incoming request details and delegates the update operation to the matching handler.
 */
@Slf4j
public final class UpdateRequestDispatcher {

  /**
   * Target schema object configuration used to determine the object type.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * Schema containing definitions of supported objects and their attributes.
   */
  private final Schema schema;
  /**
   * Registered update handlers responsible for processing specific update types.
   */
  private final List<UpdateHandler> updateHandlers;

  /**
   * Creates an instance of {@code UpdateRequestDispatcher}.
   *
   * @param requestBuilder      builds API requests for ServiceNow operations
   * @param requestExecutor     executes API requests
   * @param getRequest          Used to get data for various objects
   * @param targetSchemaObjects configuration defining the target schema objects
   * @param schema              schema definition for supported objects
   * @param batchSize           size for fetching chained data in batches
   */
  public UpdateRequestDispatcher(final RequestBuilder requestBuilder,
                                 final RequestExecutor requestExecutor,
                                 final GetRequest getRequest,
                                 final ReadOnlyProperties targetSchemaObjects,
                                 final Schema schema,
                                 final int batchSize) {
    this.targetSchemaObjects = targetSchemaObjects;
    this.schema = schema;
    this.updateHandlers = Arrays.asList(
        new UpdateUserRequestHandler(requestBuilder, requestExecutor, getRequest, batchSize),
        new UpdateGroupRequestHandler(requestBuilder, requestExecutor, batchSize),
        new UpdateRoleRequestHandler(requestBuilder, requestExecutor, batchSize)
    );
  }

  /**
   * Processes an LDAP modify request by delegating it to the appropriate {@link UpdateHandler}
   * implementation.
   *
   * @param ldapModifyRequest LDAP modify request containing update details
   * @return {@link LdapResponse} indicating success or failure of the update operation
   */
  public LdapResponse<Void> modify(final LdapModifyRequest ldapModifyRequest) {
    try {
      RequestDetails requestDetails =
          Util.getModifyRequestDetails(ldapModifyRequest);
      String uniqueKey = requestDetails.getUniqueKey();
      SchemaObject schemaObject =
          Util.getSchemaObject(schema, Util.getSchemaObjectName(targetSchemaObjects));
      for (UpdateHandler operation : updateHandlers) {
        if (operation.supports(requestDetails)) {
          operation.process(uniqueKey, ldapModifyRequest, schemaObject);
          return new LdapResponse<>(LdapResultCode.SUCCESS);
        }
      }
      // here CONSTRAINT_VIOLATION should be returned as no matching handler found, but as that is
      // currently not supported, so OPERATIONS_ERROR will be returned
      log.warn("No update handler found for request type");
    } catch (ServiceNowException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
    }
    return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
  }
}
