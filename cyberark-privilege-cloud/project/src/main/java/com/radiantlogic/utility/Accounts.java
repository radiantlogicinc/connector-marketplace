package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store fetched accounts.
 */
@Getter
@Setter
@NoArgsConstructor
public class Accounts {

  /**
   * This field will store fetched accounts.
   */
  @SerializedName("value")
  private final List<AccountRow> accountsList = new ArrayList<>();
  /**
   * This field will store get accounts pagination link.
   */
  private String nextLink;

  /**
   * This method will assign a list to accountsList member field.
   *
   * @param accounts List which has to be assigned
   */
  public void setAccountsList(final List<AccountRow> accounts) {
    this.accountsList.clear();
    if (accounts != null) {
      this.accountsList.addAll(accounts);
    }
  }

}
