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
 * Represents a single search result entry containing key-value attributes.
 *
 * <p>This class is used to store and expose attributes returned from search
 * operations in a structured map format.</p>
 */
@ToString
public class SearchResultEntry {

  /**
   * Internal map storing attribute names and their corresponding values.
   */
  private final Map<String, Object> attributes;

  /**
   * Creates an empty search result entry.
   */
  public SearchResultEntry() {
    this.attributes = new HashMap<>();
  }

  /**
   * Adds or updates an attribute in this entry.
   *
   * @param key the attribute name (must not be blank)
   * @param value the attribute value
   */
  public void put(@NonNull final String key, final Object value) {
    ValidationUtils.requireArgument(StringUtils.isNotBlank(key), "Key must not be blank.");
    this.attributes.put(key, value);
  }

  /**
   * Returns an unmodifiable view of the attributes in this entry.
   *
   * @return an unmodifiable map of attributes
   */
  @JsonValue
  public Map<String, Object> asUnmodifiableMap() {
    return Collections.unmodifiableMap(this.attributes);
  }
}
