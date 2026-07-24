package com.radiantlogic.get;

import static com.radiantlogic.utility.Constants.ACCOUNT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.BASE_SEARCH_REQUEST_KEY;
import static com.radiantlogic.utility.Constants.DATA_FETCH_FAILED_MESSAGE;
import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_KEY;
import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;
import static com.radiantlogic.utility.Constants.GOOGLE_JSON;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.ROLE_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SAFE_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SUBDOMAIN_NOT_AVAILABLE_ERROR_MESSAGE;
import static com.radiantlogic.utility.Constants.USER_ENTITY_KEY;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SequentialDataProvider;
import com.radiantlogic.iddm.ldap.LdapResponse;
import com.radiantlogic.iddm.ldap.LdapResultCode;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.misc.RequestExecutor;
import com.radiantlogic.utility.AccountRow;
import com.radiantlogic.utility.AccountUtils;
import com.radiantlogic.utility.Accounts;
import com.radiantlogic.utility.DirectoryServiceRecord;
import com.radiantlogic.utility.DirectoryServiceUtils;
import com.radiantlogic.utility.Record;
import com.radiantlogic.utility.RoleUtils;
import com.radiantlogic.utility.SafeRow;
import com.radiantlogic.utility.SafeUtils;
import com.radiantlogic.utility.SafesResult;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import com.radiantlogic.utility.UserUtils;
import com.radiantlogic.utility.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will handle getting data for paginated/non-paginated objects.
 */
@Slf4j
@SuppressWarnings(PMD_KEY)
public class PagedResponse implements SequentialDataProvider<LdapResponse<?>> {

  /**
   * This field will store whether request has been invalidated.
   */
  private boolean invalidated;
  /**
   * This field will store whether request is for user entity.
   */
  boolean isUsersRequest;
  /**
   * This field will store whether request is for role entity.
   */
  boolean isRolesRequest;
  /**
   * This field will store whether request is for safe entity.
   */
  boolean isSafesRequest;
  /**
   * This field will store whether request is for account entity.
   */
  boolean isAccountsRequest;
  /**
   * This field will store whether request is for directoryservice entity.
   */
  boolean isDirectoryServicesRequest;
  /**
   * This field will store page number used in case of pagination.
   */
  private int pageNumber;
  /**
   * This object will have request details.
   */
  private final LdapSearchRequest searchRequest;
  /**
   * This field will store whether request is for single entity record.
   */
  private boolean isFetchParticularRecordRequest;
  /**
   * This list will contain certain roles which are not part of identity administration. required
   * roles will be filtered out from this
   */
  private List<Record> rolesWithMembersList;
  /**
   * This object will be used to convert response data.
   */
  private static final SearchResponseConverter SEARCH_RESPONSE_CONVERTER =
      new SearchResponseConverter();
  /**
   * This field will store whether more records are there in case of pagination.
   */
  private boolean hasMoreRecords = true;
  /**
   * This field will store unique key in case of fetching a single record.
   */
  private final String uniqueKey;
  /**
   * This object will contain request object's name.
   */
  ReadOnlyProperties targetSchemaObjects;
  /**
   * This object will have requested object's schema.
   */
  Schema schema;
  /**
   * This object will be used to get data for various objects.
   */
  private final GetRequest getRequest;
  /**
   * This object will be used to execute a request.
   */
  private final RequestExecutor requestExecutor;
  /**
   * This field will store next link for pagination.
   */
  private String nextLink;
  /**
   * Field to store schema object name.
   */
  private final String schemaObjectName;

  /**
   * This constructor will be used to initialize class's instance members.
   *
   * @param request             LdapSearchRequest class object
   * @param targetSchemaObjects ReadOnlyProperties class object
   * @param schema              Schema class object
   * @param getRequest          GetRequest class object
   * @param requestExecutor     Executor class object
   */
  public PagedResponse(final LdapSearchRequest request,
                       final ReadOnlyProperties targetSchemaObjects, final Schema schema,
                       final GetRequest getRequest, final RequestExecutor requestExecutor) {
    this.searchRequest = request;
    this.targetSchemaObjects = targetSchemaObjects;
    this.schema = schema;
    this.getRequest = getRequest;
    this.requestExecutor = requestExecutor;
    this.schemaObjectName = Utils.getSchemaObjectName(targetSchemaObjects);
    Utils.checkWhetherPrimaryKeyAttributeIsCorrectlySet(
        Utils.getSchemaObject(schema, schemaObjectName));
    checkWhetherFetchParticularRecordRequest();
    determineWhichObjectRequest();
    this.uniqueKey = isFetchParticularRecordRequest ? Utils.getUniqueKey(searchRequest) : "";
    if (isRolesRequest && !isFetchParticularRecordRequest && !invalidated
        && requestExecutor.canConnect()) {
      rolesWithMembersList = getRequest.getRolesWithMembersList();
    }
  }

  private void checkWhetherFetchParticularRecordRequest() {
    if (BASE_SEARCH_REQUEST_KEY.equalsIgnoreCase(searchRequest.getSearchScope().name())) {
      isFetchParticularRecordRequest = true;
    }
  }

  @Override
  public LdapResponse<String> next() {
    try {
      List<SearchResultEntry> list = getNextPage();
      return new LdapResponse<>(LdapResultCode.SUCCESS, SEARCH_RESPONSE_CONVERTER.toJson(list));
    } catch (CyberArkPrivilegeCloudException | IOException e) {
      log.error(EXCEPTION_PREFIX, e);
      return StringUtils.isNotBlank(e.getMessage()) &&
          SUBDOMAIN_NOT_AVAILABLE_ERROR_MESSAGE.equalsIgnoreCase(e.getMessage()) ?
          new LdapResponse<>(LdapResultCode.UNAVAILABLE) :
          new LdapResponse<>(LdapResultCode.OPERATIONS_ERROR);
    }
  }

  @Override
  public boolean hasMore() {
    if (!isValid()) {
      throw new IllegalStateException(DATA_FETCH_FAILED_MESSAGE);
    }
    // below condition has been added as incrementing pageNumber will only be applicable in case
    // of getting paginated data for users & roles, for other objects like safes, next link will
    // be used
    if (isUsersRequest || isRolesRequest) {
      pageNumber++;
    }
    return hasMoreRecords;
  }

  @Override
  public boolean isValid() {
    return !invalidated && requestExecutor.canConnect();
  }

  @Override
  public void invalidate() {
    invalidated = true;
  }

  /**
   * This method will return roles data. In case of get all roles request , roles common to both the
   * received lists will be filtered out which will save chain requests to get members for each
   * role.
   *
   * @param rolesWithoutMembersList It will contain roles which are part of identity administration
   *                                but without members
   * @param rolesWithMembersList    It will contain certain extra roles (though with members) which
   *                                are not part of identity administration
   * @return List of roles data
   */
  private List<Map<String, Object>> extractRolesData(final List<Record> rolesWithoutMembersList,
                                                     final List<Record> rolesWithMembersList)
      throws CyberArkPrivilegeCloudException {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames = Utils.getFieldNames(
        Utils.getSchemaObject(schema, schemaObjectName));
    // rolesWithMembersList will be null when a request is to fetch details for a specific role
    if (rolesWithMembersList == null) {
      searchResultData.add(RoleUtils.getRoleMap(rolesWithoutMembersList.get(0), null, fieldNames));
    } else {
      for (Record role : rolesWithoutMembersList) {
        Record matchingRecord = rolesWithMembersList.stream()
            .filter(x -> x.getRecordRow().getId().equalsIgnoreCase(role.getRecordRow().getId()))
            .findFirst().orElse(null);

        // capturing roles which are common to both
        if (matchingRecord != null) {
          searchResultData.add(
              RoleUtils.getRoleMap(role, matchingRecord.getRecordRow().getMembersStringType(),
                  fieldNames));
        }
      }
    }
    return searchResultData;
  }

  private void determineWhichObjectRequest() {
    switch (schemaObjectName.toLowerCase(Locale.ROOT)) {
      case USER_ENTITY_KEY: {
        isUsersRequest = true;
        break;
      }
      case ROLE_ENTITY_KEY: {
        isRolesRequest = true;
        break;
      }
      case SAFE_ENTITY_KEY: {
        isSafesRequest = true;
        break;
      }
      case ACCOUNT_ENTITY_KEY: {
        isAccountsRequest = true;
        break;
      }
      case DIRECTORY_SERVICES_KEY: {
        isDirectoryServicesRequest = true;
        break;
      }
      default: {
        throw new IllegalStateException(
            String.format("invalid object received for data fetch:%s", schemaObjectName));
      }
    }
  }

  private List<SearchResultEntry> getUsers() throws IOException, CyberArkPrivilegeCloudException {
    List<Record> users = getRequest.getUsers(pageNumber, uniqueKey, null, null,
        Utils.getFilter(searchRequest, schemaObjectName));
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    log.debug("users fetched are:{}", users.size());
    if (users.isEmpty()) {
      hasMoreRecords = false;
    } else {
      pageEntries.addAll(SEARCH_RESPONSE_CONVERTER.convert(
          GOOGLE_JSON.toJson(UserUtils.extractUserData(users, schema, targetSchemaObjects))));
      if (isFetchParticularRecordRequest) {
        hasMoreRecords = false;
      }
    }
    return pageEntries;
  }

  private List<SearchResultEntry> getRoles() throws IOException, CyberArkPrivilegeCloudException {
    List<Record> roles =
        getRequest.getRoles(pageNumber, uniqueKey, true, null, null,
            RoleUtils.checkWhetherFetchRoleRights(schema, schemaObjectName),
            Utils.getFilter(searchRequest, schemaObjectName));
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    log.debug("roles fetched are:{}", roles.size());
    if (roles.isEmpty()) {
      hasMoreRecords = false;
    } else {
      pageEntries.addAll(SEARCH_RESPONSE_CONVERTER.convert(
          GOOGLE_JSON.toJson(extractRolesData(roles, rolesWithMembersList))));
      if (isFetchParticularRecordRequest) {
        hasMoreRecords = false;
      }
    }
    return pageEntries;
  }

  private List<SearchResultEntry> getNextPage()
      throws IOException, CyberArkPrivilegeCloudException {
    if (isValid()) {
      if (isUsersRequest) {
        return getUsers();
      } else if (isRolesRequest) {
        return getRoles();
      } else if (isSafesRequest) {
        return getSafes();
      } else if (isAccountsRequest) {
        return getAccounts();
      } else if (isDirectoryServicesRequest) {
        return getDirectoryServices();
      } else {
        throw new CyberArkPrivilegeCloudException("invalid object request received");
      }
    } else {
      throw new CyberArkPrivilegeCloudException(DATA_FETCH_FAILED_MESSAGE);
    }
  }

  private List<SearchResultEntry> getSafes() throws IOException, CyberArkPrivilegeCloudException {
    SafesResult safesResult = getRequest.getSafes(uniqueKey, nextLink,
        Utils.getFilter(searchRequest, schemaObjectName));
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    List<SafeRow> safes = safesResult.getSafes();
    nextLink = safesResult.getNextLink();
    log.debug("safes fetched: {}", safes.size());
    if (!safes.isEmpty()) {
      pageEntries.addAll(SEARCH_RESPONSE_CONVERTER.convert(
          GOOGLE_JSON.toJson(SafeUtils.extractSafesData(safes, schema, targetSchemaObjects))));
    }
    if (StringUtils.isBlank(nextLink)) {
      hasMoreRecords = false;
    }
    return pageEntries;
  }

  private List<SearchResultEntry> getAccounts()
      throws IOException, CyberArkPrivilegeCloudException {
    Accounts accountsResult = getRequest.getAccounts(uniqueKey, nextLink,
        Utils.getFilter(searchRequest, schemaObjectName));
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    List<AccountRow> accounts = accountsResult.getAccountsList();
    nextLink = accountsResult.getNextLink();
    log.debug("accounts fetched: {}", accounts.size());
    if (!accounts.isEmpty()) {
      pageEntries.addAll(SEARCH_RESPONSE_CONVERTER.convert(GOOGLE_JSON.toJson(
          AccountUtils.extractAccountsData(accounts, schema, targetSchemaObjects))));
    }
    if (StringUtils.isBlank(nextLink)) {
      hasMoreRecords = false;
    }
    return pageEntries;
  }

  private List<SearchResultEntry> getDirectoryServices()
      throws IOException, CyberArkPrivilegeCloudException {
    List<DirectoryServiceRecord> directoryServiceRecords =
        getRequest.getDirectoryServices(
            DirectoryServiceUtils.getUniqueKey(searchRequest, schemaObjectName, uniqueKey));
    List<SearchResultEntry> pageEntries = new LinkedList<>();
    log.debug("directories fetched: {}", directoryServiceRecords.size());
    if (!directoryServiceRecords.isEmpty()) {
      pageEntries.addAll(SEARCH_RESPONSE_CONVERTER.convert(GOOGLE_JSON.toJson(
          DirectoryServiceUtils.extractDirectoryServicesData(directoryServiceRecords, schema,
              targetSchemaObjects))));
    }
    hasMoreRecords = false;
    return pageEntries;
  }
}
