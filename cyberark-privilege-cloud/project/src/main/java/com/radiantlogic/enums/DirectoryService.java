package com.radiantlogic.enums;

import com.radiantlogic.exception.CyberArkPrivilegeCloudException;
import com.radiantlogic.utility.DirectoryServiceRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * This enum will contain directory service entity related attributes. Data source should have below
 * as part of attribute names.
 */
@Getter
public enum DirectoryService {
  SERVICE("Service", DirectoryServiceRow::getService),
  DISPLAY_NAME("DisplayName", DirectoryServiceRow::getDisplayName),
  TENANT("Tenant", DirectoryServiceRow::getTenant),
  NAME("Name", DirectoryServiceRow::getName),
  STATUS("Status", DirectoryServiceRow::getStatus),
  CONFIG("Config", DirectoryServiceRow::getConfig),
  SYNC_RULES_CONFIGURED("SyncRulesConfigured", DirectoryServiceRow::getSyncRulesConfigured),
  STATUS_DISPLAY("StatusDisplay", DirectoryServiceRow::getStatusDisplay),
  EVERYBODY("Everybody", DirectoryServiceRow::getEverybody),
  DESCRIPTION("Description", DirectoryServiceRow::getDescription),
  SYNC_JOB_STATUS("SyncJobStatus", DirectoryServiceRow::getSyncJobStatus),
  DISPLAY_NAME_SHORT("DisplayNameShort", DirectoryServiceRow::getDisplayNameShort),
  DIRECTORY_SERVICE_UUID("DirectoryServiceUuid", DirectoryServiceRow::getDirectoryServiceUuid);

  /**
   * This field will store attribute name.
   */
  private final String attributeName;
  /**
   * Function for extracting the attribute's value from a DirectoryServiceRow object.
   */
  private final Function<DirectoryServiceRow, Object> getter;
  /**
   * Constant to define an immutable map containing field names and corresponding directory service
   * object.
   */
  public static final Map<String, DirectoryService> LOOKUP =
      Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap
          (directoryService -> directoryService.attributeName.toLowerCase(Locale.ROOT)
              , directoryService -> directoryService)));

  DirectoryService(final String attributeName, final Function<DirectoryServiceRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * This method will extract directory service's particular field's/attribute's value.
   *
   * @param directoryServiceRow DirectoryServiceRow object containing value to be extracted
   * @return Extracted value
   */
  public Optional<Object> extractValue(final DirectoryServiceRow directoryServiceRow) {
    return Optional.ofNullable(getter.apply(directoryServiceRow));
  }

  /**
   * This method will check whether received fieldName is valid or not.
   *
   * @param fieldName Field name to be validated
   * @return Matched Directory Service object
   * @throws CyberArkPrivilegeCloudException In case of invalid attribute name received
   */
  public static DirectoryService fromName(final String fieldName)
      throws CyberArkPrivilegeCloudException {
    DirectoryService directoryService = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (directoryService == null) {
      throw new CyberArkPrivilegeCloudException(
          String.format("Invalid attribute name:%s", fieldName));
    }
    return directoryService;
  }
}