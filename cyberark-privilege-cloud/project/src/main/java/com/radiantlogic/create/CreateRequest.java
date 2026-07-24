package com.radiantlogic.create;

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
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.RequestDetails;
import com.radiantlogic.utility.Utils;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will contain functionality for creating objects.
 */
@Slf4j
public class CreateRequest {

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
   * This object will handle create user operation.
   */
  private final CreateUserRequest createUserRequest;
  /**
   * This object will handle create role operation.
   */
  private final CreateRoleRequest createRoleRequest;

  /**
   * This constructor will be used to initialize CreateRequest class's instance members.
   *
   * @param requestBuilder      RequestBuilder class object
   * @param getRequest          GetRequest class object
   * @param requestExecutor     Executor class object
   * @param targetSchemaObjects ReadOnlyProperties
   * @param schema              Schema class object
   */
  public CreateRequest(final RequestBuilder requestBuilder, final GetRequest getRequest,
                       final RequestExecutor requestExecutor,
                       @Property(name = InjectableProperties.TARGET_SCHEMA_OBJECTS)
                       final ReadOnlyProperties targetSchemaObjects,
                       @Property(name = InjectableProperties.SCHEMAS) final Schema schema) {
    this.requestExecutor = requestExecutor;
    this.targetSchemaObjects = targetSchemaObjects;
    this.schema = schema;
    this.createUserRequest = new CreateUserRequest(requestBuilder, requestExecutor);
    this.createRoleRequest = new CreateRoleRequest(requestBuilder, getRequest, requestExecutor);
  }

  /**
   * This method will process create object request.
   *
   * @param ldapAddRequest Contains request details
   * @return Whether create operation was successful or not
   */
  @SuppressWarnings(PMD_KEY)
  public LdapResponse<Void> create(final LdapAddRequest ldapAddRequest) {
    try {
      if (!requestExecutor.canConnect()) {
        throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
      }
      RequestDetails requestDetails = Utils.getCreateRequestDetails(ldapAddRequest);
      String uniqueKey = requestDetails.getUniqueKey();
      if (StringUtils.isNotBlank(uniqueKey)) {
        SchemaObject schemaObject =
            Utils.getSchemaObject(schema, Utils.getSchemaObjectName(targetSchemaObjects));
        if (requestDetails.isUserRequest()) {
          createUserRequest.processCreateUserRequest(ldapAddRequest, schemaObject, uniqueKey);
        } else if (requestDetails.isRoleRequest()) {
          createRoleRequest.processCreateRoleRequest(ldapAddRequest, schemaObject, uniqueKey);
        }
        return new LdapResponse<>(LdapResultCode.SUCCESS);
      }
    } catch (CyberArkPrivilegeCloudException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
    }
    return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
  }

}
