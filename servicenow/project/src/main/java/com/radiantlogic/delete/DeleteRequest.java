package com.radiantlogic.delete;

import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_SERVICENOW_MESSAGE;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.model.RequestDetails;
import com.radiantlogic.utility.Util;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to handle delete object operation.
 */
@Slf4j
public class DeleteRequest {

  /**
   * Field to handle delete user request.
   */
  private final DeleteUserRequest deleteUserRequest;
  /**
   * Field to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   * @param getRequest      Field to get data for various objects
   */
  public DeleteRequest(final RequestBuilder requestBuilder, final RequestExecutor requestExecutor,
                       final GetRequest getRequest) {
    this.requestBuilder = requestBuilder;
    this.deleteUserRequest = new DeleteUserRequest(requestBuilder, requestExecutor, getRequest);
  }

  /**
   * Method to process delete object request.
   *
   * @param ldapDeleteRequest Will contain delete request details
   * @return LdapResponse<>() Whether operation was successful or not
   */
  public LdapResponse<String> delete(final LdapDeleteRequest ldapDeleteRequest) {
    try {
      if (!requestBuilder.canConnectWithValidToken()) {
        log.error(UNABLE_TO_CONNECT_WITH_SERVICENOW_MESSAGE);
        return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
      }
      RequestDetails requestDetails =
          Util.getDeleteRequestDetails(ldapDeleteRequest);
      String uniqueKey = requestDetails.getUniqueKey();
      // added if block as more objects at a later stage
      if (requestDetails.isUserRequest()) {
        return deleteUserRequest.deleteUser(uniqueKey);
      }
    } catch (ServiceNowException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
    }
    return new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
  }
}
