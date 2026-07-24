package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.AUTHORIZATION_KEYWORD;
import static com.radiantlogic.utility.Constants.DATA_FETCH_FAILED_MESSAGE;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE;

import com.radiantlogic.enums.HttpStatus;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.misc.RequestBuilder;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.CyberArkResponse;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.RecordRow;
import com.radiantlogic.utility.ResponseData;
import com.radiantlogic.utility.RoleUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will get data for various objects.
 */
@Slf4j
public class GetRolesRequest {

  /**
   * This object will be used to get url & payload for various create objects operations.
   */
  private final RequestBuilder requestBuilder;
  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;

  /**
   * This constructor will initialize class's instance members.
   *
   * @param requestBuilder  RequestBuilder class object
   * @param requestExecutor Executor class object
   */
  public GetRolesRequest(final RequestBuilder requestBuilder,
                         final RequestExecutor requestExecutor) {
    this.requestBuilder = requestBuilder;
    this.requestExecutor = requestExecutor;
  }

  /**
   * This method will fetch specific page's roles as part of paginated response or a specific role.
   *
   * @param pageNumber                              Page for which roles to be fetched
   * @param roleName                                Role Name for which details to be fetched
   * @param isMembersToBeFetchedWhenFetchByRoleName Boolean Whether role members to be fetched or
   *                                                not
   * @param roleId                                  Role id for which details to be fetched
   * @param roleIds                                 Role ids for which details to be fetched
   * @param isFetchRoleRights                       boolean whether to fetch role rights or not
   * @param filters                                 Filters to be applied while getting roles
   * @return List of roles fetched
   * @throws IOException                     In case of making http call or processing response
   * @throws CyberArkPrivilegeCloudException In case api response is not OK
   */
  public List<Record> getRoles(final int pageNumber, final String roleName,
                               final boolean isMembersToBeFetchedWhenFetchByRoleName,
                               final String roleId, final Set<String> roleIds,
                               final boolean isFetchRoleRights, final Map<String, String> filters)
      throws IOException, CyberArkPrivilegeCloudException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getRedRockUrl())
        .post(requestBuilder.getRolesPayload(pageNumber, roleName, roleId, roleIds, filters))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get role(s) api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        return extractRolesWithAdditionalDetails(roleName, isMembersToBeFetchedWhenFetchByRoleName,
            isFetchRoleRights, cyberArkResponse);
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get roles api response message is: %s", apiResponse));
  }

  private @NonNull List<Record> extractRolesWithAdditionalDetails(
      final String roleName,
      final boolean isMembersToBeFetchedWhenFetchByRoleName,
      final boolean isFetchRoleRights,
      final CyberArkResponse cyberArkResponse)
      throws IOException, CyberArkPrivilegeCloudException {
    List<Record> roles = new ArrayList<>(cyberArkResponse.getResultData().getRecords());
    log.debug("roles fetched:{}", roles.size());
    // get members for a specific role
    checkAndFetchRoleMembers(roleName, isMembersToBeFetchedWhenFetchByRoleName, roles);
    if (isFetchRoleRights) {
      assignRolesRights(roles);
    }
    return roles;
  }

  private void checkAndFetchRoleMembers(final String roleName,
                                        final boolean isMembersToBeFetchedWhenFetchByRoleName,
                                        final List<Record> roles)
      throws IOException, CyberArkPrivilegeCloudException {
    if (StringUtils.isNotBlank(roleName) && isMembersToBeFetchedWhenFetchByRoleName
        && roles.size() == 1) {
      List<String> members = new ArrayList<>();
      for (Record member : getRoleMembers(roles.get(0).getRecordRow().getId())) {
        RecordRow recordRow = member.getRecordRow();
        // in get all roles with members also, by default format is (memberType)memberName
        members.add(String.format("(%s)%s", recordRow.getMemberType(), recordRow.getName()));
      }
      roles.get(0).getRecordRow().setMembers(members);
    }
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
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    Request request = new Request.Builder().url(requestBuilder.getRoleMembersUrl())
        .post(requestBuilder.getRoleMembersPayload(roleId))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get role members api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse = GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        List<Record> members = cyberArkResponse.getResultData().getRecords();
        log.debug("total members fetched are: {}", members.size());
        return members;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get role members api response message is: %s", apiResponse));
  }

  /**
   * This method will fetch all roles with their members. It will also include certain roles which
   * are not part of identity administration, those can be filtered out while processing
   *
   * @return List of roles with their members
   * @throws IllegalStateException In case response is not OK or in case of an issue while making
   *                               http call or processing response
   */
  @SuppressWarnings(PMD_KEY)
  public List<Record> getRolesWithMembersList() {
    if (!requestExecutor.canConnect()) {
      throw new IllegalStateException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    int recordsFetched = 0;
    List<Record> allRecords = new ArrayList<>();
    int pageNumber = 1;
    try {
      do {
        Request request = new Request.Builder().url(requestBuilder.getRedRockUrl())
            .post(requestBuilder.getRolesWithMembersListPayload(pageNumber))
            .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
        ResponseData responseData = requestExecutor.execute(request);
        int responseCode = responseData.getResponseCode();
        log.debug("get role(s) with members api response code with message is: {} {}", responseCode,
            responseData.getResponseMessage());
        String apiResponse = responseData.getApiResponse();
        if (responseCode == HttpStatus.OK.asInt()) {
          CyberArkResponse cyberArkResponse =
              GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
          if (cyberArkResponse.isSuccess()) {
            List<Record> records = cyberArkResponse.getResultData().getRecords();
            allRecords.addAll(records);
            recordsFetched = records.size();
            log.debug("get roles with members roles fetched:{}", recordsFetched);
            pageNumber++;
          } else {
            throw new CyberArkPrivilegeCloudException(
                String.format("getting roles with members failed:%s", apiResponse));
          }
        } else if (responseCode == HttpStatus.TOKEN_EXPIRED.asInt()) {
          requestExecutor.generateToken();
        } else {
          throw new CyberArkPrivilegeCloudException(
              String.format("get roles with members api response message is: %s", apiResponse));
        }
      } while (recordsFetched > 0);
      log.debug("total roles with members fetched are: {}", allRecords.size());
      return allRecords;
    } catch (CyberArkPrivilegeCloudException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
      throw new IllegalStateException(DATA_FETCH_FAILED_MESSAGE, e);
    }
  }

  /**
   * Method to assign administrative role rights to fetched roles.
   *
   * @param roles Roles to which rights to be assigned
   * @throws CyberArkPrivilegeCloudException In case api response is not ok
   * @throws IOException                     In case of connectivity error
   */
  private void assignRolesRights(final List<Record> roles)
      throws CyberArkPrivilegeCloudException, IOException {

    /*
      Design Rationale

      As there was no specific endpoint to fetch administrative role rights for multiple roles in
       a single call, so roles rights for each role will be fetched separately.
    */

    for (Record role : roles) {
      assignRoleRights(role);
    }
  }

  private void assignRoleRights(final Record role)
      throws CyberArkPrivilegeCloudException, IOException {
    if (!requestExecutor.canConnect()) {
      throw new CyberArkPrivilegeCloudException(UNABLE_TO_CONNECT_WITH_CYBERARK_MESSAGE);
    }
    HttpUrl roleRightsUrl = requestBuilder.getRoleRightsUrl();
    Request request = new Request.Builder().url(roleRightsUrl)
        .post(requestBuilder.getRoleRightsPayload(role.getRecordRow().getId()))
        .addHeader(AUTHORIZATION_KEYWORD, requestExecutor.getTokenWithBearerPrefixed()).build();
    ResponseData responseData = requestExecutor.execute(request);
    int responseCode = responseData.getResponseCode();
    log.debug("get role rights api response code with message is: {} {}", responseCode,
        responseData.getResponseMessage());
    String apiResponse = responseData.getApiResponse();
    if (responseCode == HttpStatus.OK.asInt()) {
      CyberArkResponse cyberArkResponse =
          GOOGLE_JSON.fromJson(apiResponse, CyberArkResponse.class);
      if (cyberArkResponse.isSuccess()) {
        RoleUtils.attachRoleRightsToRole(role, cyberArkResponse);
        return;
      }
    }
    throw new CyberArkPrivilegeCloudException(
        String.format("get role rights api response message is:%s", apiResponse));
  }
}
