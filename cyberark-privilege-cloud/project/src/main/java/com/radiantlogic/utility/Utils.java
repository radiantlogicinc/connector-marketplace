package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.ACCOUNT_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.ACCOUNT_FILTER_QUERY_PARAMETER_KEY;
import static com.radiantlogic.utility.Constants.ALLOWED_ENTITIES;
import static com.radiantlogic.utility.Constants.ALLOWED_PRIMARY_KEY_ATTRIBUTES;
import static com.radiantlogic.utility.Constants.ASTERISK_KEY;
import static com.radiantlogic.utility.Constants.DIRECTORY_SERVICES_KEY;
import static com.radiantlogic.utility.Constants.INVALID_SCHEMA_NAME_MESSAGE_PREFIX;
import static com.radiantlogic.utility.Constants.MAX_FILTERS_SIZE;
import static com.radiantlogic.utility.Constants.MAX_FILTER_VALUE_SIZE;
import static com.radiantlogic.utility.Constants.NULL_KEY;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.ROLE_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SAFE_ENTITY_KEY;
import static com.radiantlogic.utility.Constants.SAFE_FILTER_QUERY_PARAMETER_KEY;
import static com.radiantlogic.utility.Constants.SCHEMA_OBJECTS_KEY;
import static com.radiantlogic.utility.Constants.SIZE_ONE;
import static com.radiantlogic.utility.Constants.USER_ENTITY_KEY;

import com.radiantlogic.enums.Account;
import com.radiantlogic.enums.DirectoryService;
import com.radiantlogic.enums.Safe;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.Field;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.base.SchemaObject;
import com.radiantlogic.iddm.base.SearchFilter;
import com.radiantlogic.iddm.ldap.Attribute;
import com.radiantlogic.iddm.ldap.LdapAddRequest;
import com.radiantlogic.iddm.ldap.LdapDeleteRequest;
import com.radiantlogic.iddm.ldap.LdapModifyRequest;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import com.radiantlogic.iddm.ldap.RDN;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will contain general utility functions.
 */
@Slf4j
@SuppressWarnings(PMD_KEY)
public final class Utils {

  private Utils() {
  }

  /**
   * This method will return schema object name.
   *
   * @param targetSchemaObjects ReadOnlyProperties containing schema objects
   * @return Schema object name
   */
  public static String getSchemaObjectName(final ReadOnlyProperties targetSchemaObjects) {
    List<String> schemaObjectNames = (List<String>) targetSchemaObjects.get(SCHEMA_OBJECTS_KEY);
    if (schemaObjectNames == null || schemaObjectNames.size() != 1) {
      throw new IllegalStateException("Unable to get schema object type for current search"
          + "/Expecting IDDM to only ever send one schema object per LDAP request");
    }
    return schemaObjectNames.get(0);
  }

  /**
   * This method will return schema object.
   *
   * @param schema           Schema
   * @param schemaObjectName Schema object name
   * @return SchemaObject
   */
  public static SchemaObject getSchemaObject(final Schema schema, final String schemaObjectName) {
    return schema.getSchemaObject(schemaObjectName).orElseThrow(() -> new IllegalStateException(
        String.format("Could not find schema object:%s ", schemaObjectName)));
  }

  /**
   * This method will check whether primary key attribute is correctly set.
   *
   * @param schemaObject Schema object for which primary key attribute is to be checked
   */
  public static void checkWhetherPrimaryKeyAttributeIsCorrectlySet(
      final SchemaObject schemaObject) {
    if (schemaObject.getPrimaryKeys().size() != SIZE_ONE) {
      throw new IllegalStateException("expected single primary key attribute");
    }
    String primaryKeyAttributeName = schemaObject.getPrimaryKeys().get(0);

    if (!ALLOWED_PRIMARY_KEY_ATTRIBUTES.contains(
        primaryKeyAttributeName.toLowerCase(Locale.ROOT))) {
      throw new IllegalStateException("missing required primary key attribute name");
    }
  }

  /**
   * This method will get fields of a schema object.
   *
   * @param schemaObject Schema object for which fields to be fetched
   * @return List of fields
   */
  public static List<String> getFieldNames(final SchemaObject schemaObject) {
    return schemaObject.getFields().stream().map(Field::getName).collect(Collectors.toList());
  }

  /**
   * This method will return backend attribute name corresponding to frontend mapped name.
   *
   * @param schemaObject  Schema object containing details
   * @param attributeName MappedName for which attribute name to be fetched
   * @return Matching Attribute name
   */
  public static String getAttributeName(final SchemaObject schemaObject,
                                        final String attributeName) {
    checkWhetherPrimaryKeyAttributeIsCorrectlySet(schemaObject);
    return schemaObject.getFields().stream().filter(
        x -> x.getMappedAttrName().equalsIgnoreCase(attributeName) || x.getName()
            .equalsIgnoreCase(attributeName)).map(Field::getName).findFirst().orElse(null);
  }

  /**
   * This method will check whether attribute's value is not more than one.
   *
   * @param attribute Attribute whose values size is to be checked
   * @return true if values size is one
   * @throws CyberArkPrivilegeCloudException In case values size is not one
   */
  public static boolean isAttributeValuesSizeIsOne(final Attribute attribute)
      throws CyberArkPrivilegeCloudException {
    if (attribute.getValues().size() != SIZE_ONE) {
      throw new CyberArkPrivilegeCloudException(
          String.format("attribute values size has to be one:%s", attribute.getName()));
    }
    return true;
  }

  /**
   * This method will populate request details for create request.
   *
   * @param ldapAddRequest LdapAddRequest containing request details
   * @return RequestDetails containing create request details
   * @throws CyberArkPrivilegeCloudException In case required details are not found
   */
  @SuppressWarnings(PMD_KEY)
  public static RequestDetails getCreateRequestDetails(final LdapAddRequest ldapAddRequest)
      throws CyberArkPrivilegeCloudException {
    for (RDN component : ldapAddRequest.getDn().getRDNs()) {
      List<Attribute> attributes = component.getAttributes();
      for (Attribute attribute : attributes) {
        if (ALLOWED_ENTITIES.contains(attribute.getName().toLowerCase(Locale.ROOT))) {
          RequestDetails requestDetails = new RequestDetails();
          requestDetails.setUserRequest(USER_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setRoleRequest(ROLE_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setUniqueKey(attribute.getValues().get(0));
          return requestDetails;
        }
      }
    }
    throw new CyberArkPrivilegeCloudException("create request's required details not found");
  }

  /**
   * This method will populate request details for update request.
   *
   * @param ldapModifyRequest LdapModifyRequest containing request details
   * @return RequestDetails containing update request details
   * @throws CyberArkPrivilegeCloudException In case required details are not found
   */
  @SuppressWarnings(PMD_KEY)
  public static RequestDetails getModifyRequestDetails(final LdapModifyRequest ldapModifyRequest)
      throws CyberArkPrivilegeCloudException {
    for (RDN component : ldapModifyRequest.getDn().getRDNs()) {
      List<Attribute> attributes = component.getAttributes();
      for (Attribute attribute : attributes) {
        if (ALLOWED_ENTITIES.contains(attribute.getName().toLowerCase(Locale.ROOT))) {
          RequestDetails requestDetails = new RequestDetails();
          requestDetails.setUserRequest(USER_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setRoleRequest(ROLE_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setUniqueKey(attribute.getValues().get(0));
          return requestDetails;
        }
      }
    }
    throw new CyberArkPrivilegeCloudException("modify request's required details not found");
  }

  /**
   * This method will populate request details for delete request.
   *
   * @param ldapDeleteRequest LdapDeleteRequest containing request details
   * @return RequestDetails containing delete request details
   * @throws CyberArkPrivilegeCloudException In case required details are not found
   */
  @SuppressWarnings(PMD_KEY)
  public static RequestDetails getDeleteRequestDetails(final LdapDeleteRequest ldapDeleteRequest)
      throws CyberArkPrivilegeCloudException {
    for (RDN component : ldapDeleteRequest.getDn().getRDNs()) {
      List<Attribute> attributes = component.getAttributes();
      for (Attribute attribute : attributes) {
        if (ALLOWED_ENTITIES.contains(attribute.getName().toLowerCase(Locale.ROOT))) {
          RequestDetails requestDetails = new RequestDetails();
          requestDetails.setUserRequest(USER_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setRoleRequest(ROLE_ENTITY_KEY.equalsIgnoreCase(attribute.getName()));
          requestDetails.setUniqueKey(attribute.getValues().get(0));
          return requestDetails;
        }
      }
    }
    throw new CyberArkPrivilegeCloudException("delete request's required details not found");
  }

  /**
   * This method will extract unique key from ldap search request.
   *
   * @param searchRequest LdapSearchRequest containing search request details
   * @return Unique key for which data to be fetched
   */
  public static String getUniqueKey(final LdapSearchRequest searchRequest) {
    return searchRequest.getBaseDN().getRDNs().get(0).getAttributes().get(0).getValues().get(0);
  }

  /**
   * Method to get a filter map used to get filtered values. Only AND/EQUAL & SUBSTRING (allowed for
   * user & role objects) types are supported. Filters can be applied on specific filter types and
   * on specific attributes depending upon object. Objectclass filter is not counted towards max
   * filter size of one as schema object name will be used to determine the flow. If the filter type
   * is not from the above list/ filter size is more than one/same filter key has multiple values,
   * in these cases an empty map will be returned. Also, if a filter is having 'null' as value, that
   * will not be counted towards filters. Presence filters applicable to only user & role objects.
   *
   * @param searchRequest    Search Request object
   * @param schemaObjectName Schema name
   * @return Map of filter key - value
   * @throws CyberArkPrivilegeCloudException In case of invalid schema name
   */
  public static Map<String, String> getFilter(final LdapSearchRequest searchRequest,
                                              final String schemaObjectName)
      throws CyberArkPrivilegeCloudException {

    /*
      Design Rationale

      As LDAP queries can't be directly converted into red rock queries, so only one filter is
      supported to avoid any complexity & returning incorrect data, so if multiple filters are
      provided, then no filter will be applied.

      Though for users, data can be filtered based on
      username/id/displayname/source/mail/mobilenumber/risklevelrank/status and for roles based
      on name/id/description/roletype

      For get safes, filter based on safe name (full or partial)

      For get accounts, filter based on exact safe name

      For get directory services, filter based on exact name

      For safes/accounts/directory services - if * is part of filter value, no
      filter will be applied, backend will return all safes, iddm will apply filtering. Here also
      in case of multiple filters, no filter will be applied.
    */
    Map<String, String> resultFilter = new HashMap<>();
    Map<String, List<String>> matchingFilters = new HashMap<>();

    FiltersData filtersData = getAllFilters(searchRequest);
    List<SearchFilter> allFilters = filtersData.getAllFilters();
    Set<SearchFilter.Type> allFilterTypes = filtersData.getAllFilterTypes();

    if (!EnumSet.of(SearchFilter.Type.EQUAL, SearchFilter.Type.AND, SearchFilter.Type.SUBSTRING,
            SearchFilter.Type.PRESENCE)
        .containsAll(allFilterTypes)) {
      log.info("Only EQUAL/AND/SUBSTRING operators are supported, returning empty filter map");
      return resultFilter;
    }

    for (SearchFilter filter : allFilters) {
      if (USER_ENTITY_KEY.equalsIgnoreCase(schemaObjectName)) {
        UserUtils.populateFilterMapForUser(filter, matchingFilters);
      } else if (ROLE_ENTITY_KEY.equalsIgnoreCase(schemaObjectName)) {
        RoleUtils.populateFilterMapForRole(filter, matchingFilters);
      } else if (filter.getAttribute() != null) {
        if (filter.getAttribute().equalsIgnoreCase(getFilterKey(schemaObjectName))) {
          populateFilterMap(schemaObjectName, filter.getAssertionValue().orElse(null),
              matchingFilters);
        } else {
          if (Safe.LOOKUP.get(filter.getAttribute().toLowerCase(Locale.ROOT)) != null ||
              Account.LOOKUP.get(filter.getAttribute().toLowerCase(Locale.ROOT)) != null ||
              DirectoryService.LOOKUP.get(filter.getAttribute().toLowerCase(Locale.ROOT)) !=
                  null) {
            log.info("not allowed filter column detected: {}", filter.getAttribute());
            return resultFilter;
          }
        }
      }
    }
    populateResultFilter(matchingFilters, resultFilter);
    return resultFilter;
  }

  private static void populateResultFilter(Map<String, List<String>> matchingFilters,
                                           Map<String, String> resultFilter) {
    if (matchingFilters.size() == MAX_FILTERS_SIZE && matchingFilters.values().stream()
        .allMatch(list -> list.size() == MAX_FILTER_VALUE_SIZE)) {
      String keyName = matchingFilters.keySet().iterator().next();
      String value = matchingFilters.get(keyName).get(0);
      resultFilter.put(keyName, value);
    } else {
      log.info(
          "either no/multiple filters are there or same filter key has multiple values, no " +
              "filter applied");
    }
    log.debug("returning filter: {}", resultFilter);
  }

  private static Stream<SearchFilter> flattenSearchFilters(SearchFilter filter) {
    return Stream.concat(
        Stream.of(filter),
        filter.getComponents().stream()
            .flatMap(Utils::flattenSearchFilters)
    );
  }

  private static FiltersData getAllFilters(LdapSearchRequest searchRequest) {
    final SearchFilter searchFilter = searchRequest.getFilter();
    Deque<SearchFilter> stack = new ArrayDeque<>();
    stack.add(searchFilter);
    List<SearchFilter> allFilters = new ArrayList<>();
    Set<SearchFilter.Type> allFilterTypes = new HashSet<>();

    while (!stack.isEmpty()) {
      SearchFilter filter = stack.pop();

      allFilters.add(filter);
      allFilters.addAll(filter.getComponents().stream()
          .flatMap(Utils::flattenSearchFilters)
          .collect(Collectors.toList()));

      allFilterTypes.add(filter.getType());
      allFilterTypes.addAll(filter.getComponents().stream()
          .flatMap(Utils::flattenSearchFilters)
          .map(SearchFilter::getType)
          .collect(Collectors.toSet()));
    }

    FiltersData filtersData = new FiltersData();
    filtersData.setAllFilters(allFilters);
    filtersData.setAllFilterTypes(allFilterTypes);
    return filtersData;
  }


  private static void populateFilterMap(final String schemaObjectName,
                                        final String filterValue,
                                        final Map<String, List<String>> matchingFilters)
      throws CyberArkPrivilegeCloudException {
    if (StringUtils.isBlank(filterValue) || NULL_KEY.equalsIgnoreCase(filterValue.trim()) ||
        filterValue.contains(ASTERISK_KEY)) {
      log.info(
          "either filter value is blank/having 'null' as value/contains * as part of value");
      return;
    }
    matchingFilters.computeIfAbsent(getFilterQueryParameterName(schemaObjectName),
        values -> new ArrayList<>()).add(filterValue);
  }

  private static String getFilterKey(final String schemaObjectName)
      throws CyberArkPrivilegeCloudException {
    String filterKey;
    switch (schemaObjectName) {
      case SAFE_ENTITY_KEY: {
        filterKey = Safe.NAME.getAttributeName();
        break;
      }
      case ACCOUNT_ENTITY_KEY: {
        filterKey = Account.SAFE_NAME.getAttributeName();
        break;
      }
      case DIRECTORY_SERVICES_KEY: {
        filterKey = DirectoryService.NAME.getAttributeName();
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format(INVALID_SCHEMA_NAME_MESSAGE_PREFIX, schemaObjectName));
      }
    }
    return filterKey;
  }

  private static String getFilterQueryParameterName(final String schemaObjectName)
      throws CyberArkPrivilegeCloudException {
    String queryParameterKey;
    switch (schemaObjectName) {
      case SAFE_ENTITY_KEY: {
        queryParameterKey = SAFE_FILTER_QUERY_PARAMETER_KEY;
        break;
      }
      case ACCOUNT_ENTITY_KEY: {
        queryParameterKey = ACCOUNT_FILTER_QUERY_PARAMETER_KEY;
        break;
      }
      case DIRECTORY_SERVICES_KEY: {
        queryParameterKey = DirectoryService.NAME.getAttributeName();
        break;
      }
      default: {
        throw new CyberArkPrivilegeCloudException(
            String.format(INVALID_SCHEMA_NAME_MESSAGE_PREFIX, schemaObjectName));
      }
    }
    return queryParameterKey;
  }
}
