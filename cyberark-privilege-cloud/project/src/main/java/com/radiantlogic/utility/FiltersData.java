package com.radiantlogic.utility;

import com.radiantlogic.iddm.base.SearchFilter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Class to store Search Filters data.
 */
@Getter
@NoArgsConstructor
public class FiltersData {

  /**
   * List to store all deeply nested filters.
   */
  private final List<SearchFilter> allFilters = new ArrayList<>();
  /**
   * Set to store all filter types.
   */
  private final Set<SearchFilter.Type> allFilterTypes = EnumSet.noneOf(SearchFilter.Type.class);

  /**
   * Method to assign filters list to field allFilters.
   *
   * @param filters List of filters to be assigned
   */
  public void setAllFilters(final List<SearchFilter> filters) {
    this.allFilters.clear();
    if (filters != null) {
      this.allFilters.addAll(filters);
    }
  }

  /**
   * Method to assign filter types list to field allFiltersTypes.
   *
   * @param types List of filter types to be assigned
   */
  public void setAllFilterTypes(final Set<SearchFilter.Type> types) {
    this.allFilterTypes.clear();
    if (types != null) {
      this.allFilterTypes.addAll(types);
    }
  }
}
