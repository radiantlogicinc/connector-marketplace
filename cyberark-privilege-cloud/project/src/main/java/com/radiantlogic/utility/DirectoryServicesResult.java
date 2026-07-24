package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will store directory services.
 */
@Getter
@Setter
@NoArgsConstructor
public class DirectoryServicesResult {

  /**
   * This field will store directory services.
   */
  @SerializedName("Results")
  private final List<DirectoryServiceRecord> results = new ArrayList<>();

  /**
   * This method will assign a list to results member field.
   *
   * @param results List which has to be assigned
   */
  public void setResults(final List<DirectoryServiceRecord> results) {
    this.results.clear();
    if (results != null) {
      this.results.addAll(results);
    }
  }
}
