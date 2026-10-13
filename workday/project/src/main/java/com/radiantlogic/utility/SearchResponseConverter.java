package com.radiantlogic.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiantlogic.iddm.base.TypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts API response data into internal search result representations.
 *
 * <p>Responsible for transforming raw response payloads into a list of
 * {@link SearchResultEntry} objects.</p>
 */
@Slf4j
@NoArgsConstructor
public class SearchResponseConverter
    implements TypeConverter<List<Map<String, Object>>, List<SearchResultEntry>> {

  /**
   * Shared Jackson ObjectMapper instance used for JSON processing.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Message indicating that there is no data available for conversion.
   */
  private static final String NO_DATA_TO_CONVERT_MESSAGE = "no data to convert";


  /**
   * Converts a list of raw entry maps into search result entries without JSON serialization.
   *
   * <p>Avoids unnecessary JSON round-tripping by directly mapping structured data.</p>
   *
   * @param entries list of raw key-value maps representing response entries
   * @return list of {@link SearchResultEntry} objects
   */
  @Override
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
   * Converts a list of search result entries into a JSON string.
   *
   * @param entries list of search result entries to be serialized
   * @return JSON representation of the input entries
   * @throws JsonProcessingException if serialization fails
   */
  public String toJson(final @NonNull List<SearchResultEntry> entries)
      throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(entries);
  }
}
