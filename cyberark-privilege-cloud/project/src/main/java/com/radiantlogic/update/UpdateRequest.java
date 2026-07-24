package com.radiantlogic.update;

import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.annotations.Property;
import com.radiantlogic.iddm.base.InjectableProperties;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.RequestDetails;
import com.radiantlogic.utility.Utils;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * This class will handle update object related operations.
 */
@Slf4j
public class UpdateRequest {

  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * This object will be used to read target schema objects.
   */
  private final ReadOnlyProperties targetSchemaObjects;
  /**
   * This object will be used to fetch schema details corresponding to request made.
   */
  private final Schema schema;
  /**
   * This object will handle update user request.
   */
  private final UpdateUserRequest updateUserRequest;
  /**
   * This object will handle update role request.
   */
  private final UpdateRoleRequest updateRoleRequest;

  /**
   * This constructor will initialize class's instance members.
   *
   * @param requestBuilder      RequestBuilder class object
   * @param getRequest          GetRequest class object
   * @param requestExecutor     Executor class object
   * @param targetSchemaObjects ReadOnlyProperties
   * @param schema              Schema class object
   */
  public UpdateRequest(final RequestBuilder requestBuilder, final GetRequest getRequest,
                       final RequestExecutor requestExecutor,
                       @Property(name = InjectableProperties.TARGET_SCHEMA_OBJECTS)
                       final ReadOnlyProperties targetSchemaObjects,
                       @Property(name = InjectableProperties.SCHEMAS) final Schema schema) {
    this.requestExecutor = requestExecutor;
    this.targetSchemaObjects = targetSchemaObjects;
    this.schema = schema;
    this.updateUserRequest = new UpdateUserRequest(getRequest, requestBuilder, requestExecutor);
    this.updateRoleRequest = new UpdateRoleRequest(getRequest, requestBuilder, requestExecutor);
  }

  /**
   * This method will process update request.
   *
   * @param ldapModifyRequest Will contain update request details
   * @return Whether update was successful or not
   */
  @SuppressWarnings(PMD_KEY)
  public LdapResponse<Void> modify(final LdapModifyRequest ldapModifyRequest) {
    try {
      if (!requestExecutor.canConnect()) {
        throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
      }
      RequestDetails requestDetails = Utils.getModifyRequestDetails(ldapModifyRequest);
      String uniqueKey = requestDetails.getUniqueKey();
      SchemaObject schemaObject =
          Utils.getSchemaObject(schema, Utils.getSchemaObjectName(targetSchemaObjects));
      if (requestDetails.isUserRequest()) {
        updateUserRequest.processUserUpdate(uniqueKey, ldapModifyRequest, schemaObject);
      } else if (requestDetails.isRoleRequest()) {
        updateRoleRequest.processRoleUpdate(uniqueKey, ldapModifyRequest, schemaObject);
      }
      return new LdapResponse<>(LdapResultCode.SUCCESS);
    } catch (CyberArkPrivilegeCloudException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
      return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
    }
  }
}
