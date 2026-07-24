package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.DATE_TIME_FORMATTER;
import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.ZONE;

import com.radiantlogic.enums.Account;
import com.radiantlogic.iddm.base.ReadOnlyProperties;
import com.radiantlogic.iddm.base.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This class will contain account related utility methods.
 */
@Slf4j
public final class AccountUtils {

  private AccountUtils() {
  }

  /**
   * This method will convert fetched accounts data into a list of maps.
   *
   * @param accounts     Fetched accounts data
   * @param schema       Account schema
   * @param targetSchema Account target schema
   * @return List of maps of account
   */
  public static List<Map<String, Object>> extractAccountsData(
      final List<AccountRow> accounts, final Schema schema, final ReadOnlyProperties targetSchema) {
    List<Map<String, Object>> searchResultData = new ArrayList<>();
    List<String> fieldNames =
        Utils.getFieldNames(Utils.getSchemaObject(schema, Utils.getSchemaObjectName(targetSchema)));
    for (AccountRow account : accounts) {
      Map<String, Object> entry = populateAccountMap(account, fieldNames);
      searchResultData.add(entry);
    }
    return searchResultData;
  }

  @SuppressWarnings(PMD_KEY)
  private static Map<String, Object> populateAccountMap(final AccountRow accountRow,
                                                        final List<String> fieldNames) {
    Map<String, Object> entry = new HashMap<>();
    for (String fieldName : fieldNames) {
      // below code has been used as an alternative to having a catch block with case block for
      // each field name, to ensure that fields which are part of schema, value only for those
      // should be returned
      Account accountAttribute = Account.fromName(fieldName);
      Object value = accountAttribute.extractValue(accountRow);
      if (value != null) {
        entry.put(accountAttribute.getAttributeName(), value);
      }
    }
    return entry;
  }

  /**
   * This method will format accounts dates.
   *
   * @param accounts List if accounts for whom dates to be formatted
   */
  public static void formatAccountsDates(final List<AccountRow> accounts) {
    for (AccountRow account : accounts) {
      if (account.getCategoryModificationTime() != null) {
        account.setFormattedCategoryModificationTime(
            Instant.ofEpochSecond(account.getCategoryModificationTime()).atZone(ZONE)
                .format(DATE_TIME_FORMATTER));
      }
      account.setFormattedCreatedTime(
          Instant.ofEpochSecond(account.getCreatedTime()).atZone(ZONE).format(DATE_TIME_FORMATTER));

      SecretManagement secretManagement = account.getSecretManagement();
      if (secretManagement != null && secretManagement.getLastModifiedTime() != null) {
        secretManagement.setLastModifiedDateTime(
            Instant.ofEpochSecond(secretManagement.getLastModifiedTime()).atZone(ZONE)
                .format(DATE_TIME_FORMATTER));
        secretManagement.setLastModifiedTime(null); // to avoid rendering original epoch
      }
    }
  }
}