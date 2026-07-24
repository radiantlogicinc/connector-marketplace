package com.radiantlogic.utility;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class will parse get safe members api response.
 */
@Getter
@Setter
@NoArgsConstructor
public class SafeMembers {

  /**
   * This field will parse fetched safe members.
   */
  @SerializedName("value")
  private final List<SafeMember> members = new ArrayList<>();
  /**
   * This field will parse next link for pagination.
   */
  private String nextLink;

  /**
   * This method will assign a list to members field.
   *
   * @param members List which has to be assigned
   */
  public void setMembers(final List<SafeMember> members) {
    this.members.clear();
    if (members != null) {
      this.members.addAll(members);
    }
  }
}