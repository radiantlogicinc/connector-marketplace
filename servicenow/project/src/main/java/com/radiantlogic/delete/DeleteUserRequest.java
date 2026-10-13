package com.radiantlogic.delete;

import com.radiantlogic.exception.ServiceNowException;
import com.radiantlogic.get.GetRequest;
import com.radiantlogic.http.RequestBuilder;
import com.radiantlogic.http.RequestExecutor;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.model.ServiceNowResponse;
import com.radiantlogic.model.UserRow;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

/**
 * Class to handle delete user object operation.
 */
@Slf4j
public class DeleteUserRequest {

  /**
   * Field to get request object for various objects & also credentials checker.
   */
  private final RequestBuilder requestBuilder;
  /**
   * Field to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * Field to get data for various objects.
   */
  private final GetRequest getRequest;

  /**
   * Constructor to initialize instance members.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   * @param getRequest      Field to get data for various objects
   */
  public DeleteUserRequest(final RequestBuilder requestBuilder,
                           final RequestExecutor requestExecutor, final GetRequest getRequest) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
    this.getRequest = getRequest;
  }

  /**
   * Method to perform delete user operation.
   *
   * @param username Username of the user to be deleted
   * @throws IOException         In case of connectivity error
   * @throws ServiceNowException In case username is invalid/api response is not ok/In case link
   *                             header is malformed
   */
  public LdapResponse<String> deleteUser(final String username)
      throws IOException, ServiceNowException {
    ServiceNowResponse<UserRow> usersResponse = getRequest.getUsers(null, username, false);
    List<UserRow> users = usersResponse.getResult();
    if (users.isEmpty()) {
      throw new ServiceNowException(String.format("no user found with username: %s", username));
    }
    String sysId = users.get(0).getSysId();
    Request request = requestBuilder.getDeleteUserRequest(sysId);
    requestExecutor.executeDelete(request);
    log.debug("user with sys id: {} deleted", sysId);
    return new LdapResponse<>(LdapResultCode.SUCCESS);
  }
}
