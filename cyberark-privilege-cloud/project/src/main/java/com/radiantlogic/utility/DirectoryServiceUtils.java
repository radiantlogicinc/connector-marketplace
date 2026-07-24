package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.radiantlogic.enums.DirectoryService;
import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import com.radiantlogic.iddm.ldap.LdapSearchRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will contain directory service related utility methods.
 */
@Slf4j
public final class DirectoryServiceUtils {

  private DirectoryServiceUtils() {
  }

  /**
   * This method will convert fetched directory services data into a list of maps.
   *
   * @param directoryServiceRecords Fetched directory services data
   * @param schema                  Directory service schema
   * @param targetSchema            Directory service target schema
   * @return List of maps of Directory services
   * @throws CyberArkPrivilegeCloudException In case of invalid attribute name received.
   */
  public static List<Map<String, Object>> extractDirectoryServicesData(
      final List<DirectoryServiceRecord> directoryServiceRecords, final Schema schema,
      final ReadOnlyProperties targetSchema) throws CyberArkPrivilegeCloudException {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Utils.getFieldNames(Utils.getSchemaObject(schema, Utils.getSchemaObjectName(targetSchema)));
    for (DirectoryServiceRecord directoryServiceRecord : directoryServiceRecords) {
      Map<String, Object> entry =
          populateDirectoryServiceMap(directoryServiceRecord.getRow(), fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  @SuppressWarnings(PMD_KEY)
  private static Map<String, Object> populateDirectoryServiceMap(
      final DirectoryServiceRow directoryServiceRow,
      final List<String> fieldNames) throws CyberArkPrivilegeCloudException {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      // Get the value for only fields that are part of the schema.
      DirectoryService directoryService = DirectoryService.fromName(fieldName);
      directoryService.extractValue(directoryServiceRow)
          .ifPresent(value -> entry.put(directoryService.getAttributeName(), value));
    }
    return entry;
  }

  /**
   * This method will check whether the required fields as part of get directory services response
   * are not null.
   *
   * @param directoryServiceResponse Directory services api response
   * @throws CyberArkPrivilegeCloudException In case any of the required field(s) of get directory
   *                                         services response is null
   */
  public static void checkWhetherDirectoryServiceResponseIsNotNull(
      final DirectoryServiceResponse directoryServiceResponse)
      throws CyberArkPrivilegeCloudException {
    if (directoryServiceResponse == null
        || directoryServiceResponse.getResult() == null
        || directoryServiceResponse.getResult().getResults() == null) {
      throw new CyberArkPrivilegeCloudException("Directory service response is null or incomplete");
    }
  }

  /**
   * Method to get unique key based on filter value or base dn value.
   *
   * @param searchRequest    Search request object containing search details
   * @param schemaObjectName Schema name
   * @param baseSearchKey    Base dn key
   * @return Unique Key to fetch a single directory service
   * @throws CyberArkPrivilegeCloudException In case invalid schema name is provided
   */
  public static String getUniqueKey(final LdapSearchRequest searchRequest,
                                    final String schemaObjectName,
                                    final String baseSearchKey)
      throws CyberArkPrivilegeCloudException {
    Map<String, String> filters = Utils.getFilter(searchRequest, schemaObjectName);
    String filterValue = "";
    if (!filters.isEmpty()) {
      String keyName = filters.entrySet().iterator().next().getKey();
      filterValue = filters.get(keyName);
    }
    return StringUtils.isNotBlank(filterValue) ? filterValue : baseSearchKey;
  }
}