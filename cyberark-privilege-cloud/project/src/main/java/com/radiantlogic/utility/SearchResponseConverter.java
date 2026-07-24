package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiantlogic.iddm.annotations.ManagedComponent;
import com.radiantlogic.iddm.base.TypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will handle converting api response data format.
 */
@Slf4j
@ManagedComponent
@RequiredArgsConstructor
public class SearchResponseConverter implements TypeConverter<String, List<SearchResultEntry>> {

  @Override
  @SuppressWarnings(PMD_KEY)
  public List<SearchResultEntry> convert(final @NonNull String jsonData) {
    if (StringUtils.isBlank(jsonData)) {
      log.info("no data to convert");
      return new ArrayList<>();
    }

    List<Map<String, Object>> entries;
    try {
      entries =
          new ObjectMapper().readValue(jsonData, new TypeReference<List<Map<String, Object>>>() {
          });
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }

    List<SearchResultEntry> searchResults = new ArrayList<>(entries.size());
    for (Map<String, Object> entry : entries) {
      SearchResultEntry searchResultEntry = new SearchResultEntry();
      for (Map.Entry<String, Object> attribute : entry.entrySet()) {
        searchResultEntry.put(attribute.getKey(), attribute.getValue());
      }
      searchResults.add(searchResultEntry);
    }
    return searchResults;
  }

  /**
   * This method will convert data into json string format.
   *
   * @param entries List of data to be converted
   * @return Json String
   * @throws JsonProcessingException In case of parsing exception
   */
  public String toJson(final @NonNull List<SearchResultEntry> entries)
      throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(entries);
  }
}