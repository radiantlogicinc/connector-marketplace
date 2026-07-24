package com.radiantlogic.misc;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.radiantlogic.utility.SearchResultEntry;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings(PMD_KEY)
class SearchResultEntryTest {

  @Test
  void testPutAndRetrieve() {
    SearchResultEntry entry = new SearchResultEntry();
    entry.put("Username", "john.doe");
    entry.put("Email", "john@example.com");

    Map<String, Object> map = entry.asUnmodifiableMap();
    assertEquals(2, map.size(), "");
    assertEquals("john.doe", map.get("Username"), "");
    assertEquals("john@example.com", map.get("Email"), "");
  }

  @Test
  void testPutBlankKeyThrowsException() {
    SearchResultEntry entry = new SearchResultEntry();
    assertThrows(IllegalArgumentException.class, () -> entry.put("", "value"));
    assertThrows(IllegalArgumentException.class, () -> entry.put("   ", "value"));
  }

  @Test
  void testAsUnmodifiableMapIsImmutable() {
    SearchResultEntry entry = new SearchResultEntry();
    entry.put("username", "john.doe");

    Map<String, Object> map = entry.asUnmodifiableMap();
    assertThrows(UnsupportedOperationException.class, () -> map.put("email", "john@example.com"));
  }

  @Test
  void testEmptyConstructor() {
    SearchResultEntry entry = new SearchResultEntry();
    Map<String, Object> map = entry.asUnmodifiableMap();
    assertTrue(map.isEmpty(), "");
  }

}
