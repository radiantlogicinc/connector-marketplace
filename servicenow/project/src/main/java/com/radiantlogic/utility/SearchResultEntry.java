package com.radiantlogic.utility;

import com.fasterxml.jackson.annotation.JsonValue;
import com.radiantlogic.iddm.util.ValidationUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to handle search entry's data.
 */
@ToString
public class SearchResultEntry {

  /**
   * Map to store attributes and values for an entry.
   */
  private final Map<String, Object> attributes;

  /**
   * Constructor to initialize instance members.
   */
  public SearchResultEntry() {
    this.attributes = new HashMap<>();
  }

  /**
   * Method to put a key value in attributes map.
   *
   * @param key   Key name
   * @param value Value
   */
  public void put(@NonNull final String key, final Object value) {
    ValidationUtils.requireArgument(StringUtils.isNotBlank(key), "Key must not be blank.");
    this.attributes.put(key, value);
  }

  /**
   * Method to return unmodifiable version of attributes map.
   *
   * @return Map
   */
  @JsonValue
  public Map<String, Object> asUnmodifiableMap() {
    return Collections.unmodifiableMap(this.attributes);
  }
}
