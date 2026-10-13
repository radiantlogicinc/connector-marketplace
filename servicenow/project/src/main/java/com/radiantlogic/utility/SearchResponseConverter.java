package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.EXCEPTION_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiantlogic.iddm.annotations.ManagedComponent;
import com.radiantlogic.iddm.base.TypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to handle converting api response data format.
 */
@Slf4j
@ManagedComponent
@RequiredArgsConstructor
public class SearchResponseConverter implements TypeConverter<String, List<SearchResultEntry>> {

  /**
   * Shared ObjectMapper instance.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Field to define no data to convert message.
   */
  private static final String NO_DATA_TO_CONVERT_MESSAGE = "no data to convert";

  @Override
  public List<SearchResultEntry> convert(final @NonNull String jsonData) {
    if (StringUtils.isBlank(jsonData)) {
      log.info(NO_DATA_TO_CONVERT_MESSAGE);
      return new ArrayList<>();
    }
    try {
      List<Map<String, Object>> entries =
          OBJECT_MAPPER.readValue(
              jsonData,
              new TypeReference<List<Map<String, Object>>>() {
              }
          );
      return convert(entries);
    } catch (JsonProcessingException ex) {
      log.error(EXCEPTION_PREFIX, ex);
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Method to convert data format & avoid JSON round-trip.
   */
  public List<SearchResultEntry> convert(final @NonNull List<Map<String, Object>> entries) {
    if (entries.isEmpty()) {
      log.info(NO_DATA_TO_CONVERT_MESSAGE);
      return new ArrayList<>();
    }
    return entries.stream()
        .map(entry -> {
          SearchResultEntry result = new SearchResultEntry();
          entry.forEach(result::put);
          return result;
        })
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Method to convert data into json string format.
   *
   * @param entries List of data to be converted
   * @return Json String
   * @throws JsonProcessingException In case of parsing exception
   */
  public String toJson(final @NonNull List<SearchResultEntry> entries)
      throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(entries);
  }
}
