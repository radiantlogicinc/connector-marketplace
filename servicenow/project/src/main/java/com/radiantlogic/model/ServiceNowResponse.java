package com.radiantlogic.model;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class to store servicenow api response with next link for various objects.
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceNowResponse<T> {

  /**
   * Field to store url to get next set of paginated data.
   */
  private String nextLink;
  /**
   * Field to store fetched records.
   */
  private List<T> result;
}
