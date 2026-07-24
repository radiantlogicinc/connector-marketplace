package com.radiantlogic.delete;

import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
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
 * This class will handle delete object's operations.
 */
@Slf4j
public class DeleteRequest {

  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * This object will handle delete role request.
   */
  private final DeleteRoleRequest deleteRoleRequest;
  /**
   * This object will handle delete user request.
   */
  private final DeleteUserRequest deleteUserRequest;

  /**
   * This constructor will initialize instance members of the class.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param getRequest      GetRequest class object
   * @param requestExecutor Executor class object
   */
  public DeleteRequest(final RequestBuilder requestBuilder, final GetRequest getRequest,
                       final RequestExecutor requestExecutor) {
    this.requestExecutor = requestExecutor;
    this.deleteRoleRequest = new DeleteRoleRequest(requestBuilder, getRequest, requestExecutor);
    this.deleteUserRequest = new DeleteUserRequest(requestBuilder, getRequest, requestExecutor);
  }

  /**
   * This method will process delete object request.
   *
   * @param ldapDeleteRequest Will contain delete request details
   * @return Whether operation was successful or not
   */
  @SuppressWarnings(PMD_KEY)
  public LdapResponse<String> delete(final LdapDeleteRequest ldapDeleteRequest) {
    try {
      if (!requestExecutor.canConnect()) {
        throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
      }
      RequestDetails requestDetails = Utils.getDeleteRequestDetails(ldapDeleteRequest);
      String uniqueKey = requestDetails.getUniqueKey();
      if (StringUtils.isNotBlank(uniqueKey)) {
        if (requestDetails.isUserRequest()) {
          return deleteUserRequest.fetchAndDeleteUser(uniqueKey);
        } else if (requestDetails.isRoleRequest()) {
          return deleteRoleRequest.fetchAndDeleteRole(uniqueKey);
        }
      }
      log.error("key for deletion not found");

    } catch (CyberArkPrivilegeCloudException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
    }
    return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
  }
}
