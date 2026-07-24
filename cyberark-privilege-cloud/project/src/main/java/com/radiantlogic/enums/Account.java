package com.radiantlogic.enums;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.radiantlogic.utility.AccountRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * This enum will contain account entity related attributes. Data source should have below as part
 * of attribute names.
 */
@Getter
@SuppressWarnings(PMD_KEY)
public enum Account {
  NAME("Name", AccountRow::getName),
  ID("Id", AccountRow::getId),
  ADDRESS("Address", AccountRow::getAddress),
  USER_NAME("UserName", AccountRow::getUserName),
  PLATFORM_ID("PlatformId", AccountRow::getPlatformId),
  SAFE_NAME("SafeName", AccountRow::getSafeName),
  SECRET_TYPE("SecretType", AccountRow::getSecretType),
  CREATED_TIME("CreatedTime", AccountRow::getFormattedCreatedTime),
  SECRET_MANAGEMENT("SecretManagement", AccountRow::getSecretManagement),
  PLATFORM_ACCOUNT_PROPERTIES("PlatformAccountProperties",
      AccountRow::getPlatformAccountProperties),
  CATEGORY_MODIFICATION_TIME("CategoryModificationTime",
      AccountRow::getFormattedCategoryModificationTime),
  REMOTE_MACHINE_ACCESS("RemoteMachinesAccess", AccountRow::getRemoteMachinesAccess);

  /**
   * This field will store attribute name.
   */
  private final String attributeName;
  /**
   * This field will extract attribute's value.
   */
  private final Function<AccountRow, Object> getter;
  /**
   * Constant to define an immutable map containing field names and corresponding Account object.
   */
  public static final Map<String, Account> LOOKUP =
      Collections.unmodifiableMap(Arrays.stream(values())
          .collect(Collectors.toMap(account -> account.attributeName.toLowerCase(Locale.ROOT)
              , account -> account)));

  Account(final String attributeName, final Function<AccountRow, Object> getter) {
    this.attributeName = attributeName;
    this.getter = getter;
  }

  /**
   * This method will extract account's particular field's/attribute's value.
   *
   * @param accountRow AccountRow object containing value to be extracted
   * @return Extracted value
   */
  public Object extractValue(final AccountRow accountRow) {
    return getter.apply(accountRow);
  }

  /**
   * This method will check whether received fieldName is valid or not.
   *
   * @param fieldName Field name to be validated
   * @return Matched Account object
   */
  public static Account fromName(final String fieldName) {
    Account account = LOOKUP.get(fieldName.toLowerCase(Locale.ROOT));
    if (account == null) {
      throw new IllegalStateException(String.format("Invalid attribute name: %s", fieldName));
    }
    return account;
  }
}