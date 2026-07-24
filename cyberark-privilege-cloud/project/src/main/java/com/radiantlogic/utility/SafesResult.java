package com.radiantlogic.utility;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse safes result.
 */
@Getter
@Setter
@NoArgsConstructor
public class SafesResult {

  /**
   * This field will store safes.
   */
  private final List<SafeRow> safes = new ArrayList<>();
  /**
   * This field will store next link for pagination.
   */
  private String nextLink;

  /**
   * This method will assign a list to safes member field.
   *
   * @param safes List which has to be assigned
   */
  public void setSafes(final List<SafeRow> safes) {
    this.safes.clear();
    if (safes != null) {
      this.safes.addAll(safes);
    }
  }
}