package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse data fetched/role creation api response.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings(PMD_KEY)
public class ResultData {

  /**
   * This field will parse records fetched.
   */
  @SerializedName("Results")
  private final List<Record> records = new ArrayList<>();
  /**
   * This field will parse create role's id.
   */
  @SerializedName("_RowKey")
  private String id;

  /**
   * This method will assign a list to records member field.
   *
   * @param records List which has to be assigned
   */
  public void setRecords(final List<Record> records) {
    this.records.clear();
    if (records != null) {
      this.records.addAll(records);
    }
  }
}
