package com.radiantlogic.get;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.Accounts;
import com.radiantlogic.utility.DirectoryServiceRecord;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.SafesResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * This class will get data for various objects.
 */
@Slf4j
public class GetRequest {

  /**
   * This object will handle get roles request.
   */
  private final GetRolesRequest getRolesRequest;
  /**
   * This object will handle get users request.
   */
  private final GetUsersRequest getUsersRequest;
  /**
   * This object will handle get safes request.
   */
  private final GetSafesRequest getSafesRequest;
  /**
   * This object will handle get accounts request.
   */
  private final GetAccountsRequest getAccountsRequest;
  /**
   * This object will handle get Directory services request.
   */
  private final GetDirectoryServicesRequest getDirectoryServicesRequest;

  /**
   * This constructor will initialize class's instance members.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   */
  public GetRequest(final RequestBuilder requestBuilder, final RequestExecutor requestExecutor) {
    this.getRolesRequest = new GetRolesRequest(requestBuilder, requestExecutor);
    this.getUsersRequest = new GetUsersRequest(requestBuilder, requestExecutor);
    this.getSafesRequest = new GetSafesRequest(requestBuilder, requestExecutor);
    this.getAccountsRequest = new GetAccountsRequest(requestBuilder, requestExecutor);
    this.getDirectoryServicesRequest =
        new GetDirectoryServicesRequest(requestBuilder, requestExecutor);
  }

  /**
   * This method will get roles.
   *
   * @param pageNumber                              Page number for pagination
   * @param roleName                                Role name to be fetched
   * @param isMembersToBeFetchedWhenFetchByRoleName Whether role members to be fetched
   * @param roleId                                  Role id of role to be fetched
   * @param roleIds                                 Role ids of roles to be fetched
   * @param filters                                 Filters to be applied while getting roles
   * @param isFetchRoleRights                       boolean whether to fetch role rights or not
   * @return List of roles fetched
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  public List<Record> getRoles(final int pageNumber, final String roleName,
                               final boolean isMembersToBeFetchedWhenFetchByRoleName,
                               final String roleId, final Set<String> roleIds,
                               final boolean isFetchRoleRights, final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    return getRolesRequest.getRoles(pageNumber, roleName, isMembersToBeFetchedWhenFetchByRoleName,
        roleId, roleIds, isFetchRoleRights, filters);
  }

  /**
   * This method will fetch all roles with their members. It will also include certain roles which
   * are not part of identity administration, those can be filtered out while processing
   *
   * @return List of roles with their members
   * @throws IllegalStateException In case response is not OK or in case of an issue while making
   *                               http call or processing response
   */
  public List<Record> getRolesWithMembersList() {
    return getRolesRequest.getRolesWithMembersList();
  }

  /**
   * This method will get role members.
   *
   * @param roleId Role id for which members to be fetched
   * @return List of role members
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  public List<Record> getRoleMembers(final String roleId)
      throws IOException, CyberArkPrivilegeCloudException {
    return getRolesRequest.getRoleMembers(roleId);
  }

  /**
   * This method will get users.
   *
   * @param pageNumber Page for which users to be fetched
   * @param username   Username for which details to be fetched
   * @param userId     User id for which details to be fetched
   * @param userIds    User ids for which details to be fetched
   * @param filters    Filters to be applied while getting users
   * @return List of users fetched
   * @throws IOException                     In case of making http call or processing response
   * @throws CyberArkPrivilegeCloudException In case api response is not OK
   */
  public List<Record> getUsers(final int pageNumber, final String username, final String userId,
                               final Set<String> userIds, final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    return getUsersRequest.getUsers(pageNumber, username, userId, userIds, filters);
  }

  /**
   * This method will get safe(s).
   *
   * @param safeName Safe name for whom details to be fetched
   * @param nextLink Next link for pagination
   * @param filters  Filters to be applied while getting safes
   * @return SafesResult containing fetched safes & next link for pagination
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  public SafesResult getSafes(final String safeName, final String nextLink,
                              final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    return getSafesRequest.getSafes(safeName, nextLink, filters);
  }

  /**
   * This method will get account(s).
   *
   * @param accountId If an account id is provided, then details for that account will be returned,
   *                  otherwise all accounts will be returned
   * @param nextLink  Next link for pagination
   * @param filters   Filters to be applied while getting accounts
   * @return Accounts Containing fetched accounts & next link for pagination
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  public Accounts getAccounts(final String accountId, final String nextLink,
                              final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    return getAccountsRequest.getAccounts(accountId, nextLink, filters);
  }

  /**
   * This method will return directory service(s).
   *
   * @param directoryServiceName If passed, will return matching directory service, else will return
   *                             all
   * @return List of directory services fetched
   * @throws IOException                     In case of connectivity error
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   */
  public List<DirectoryServiceRecord> getDirectoryServices(final String directoryServiceName)
      throws IOException, CyberArkPrivilegeCloudException {

    /*
      Design Rationale

      As there was no specific endpoint to fetch a single directory service, so get all directory
      services endpoint (non paginated) will be called even when request is to fetch a single
      directory service and then matching record will be filtered out based on name provided.
    */

    return getDirectoryServicesRequest.getDirectoryServices(directoryServiceName);
  }

}
