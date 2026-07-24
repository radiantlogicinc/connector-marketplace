package com.radiantlogic.misc;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiantlogic.utility.SearchResponseConverter;
import com.radiantlogic.utility.SearchResultEntry;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings(PMD_KEY)
class SearchResponseConverterTest {

  SearchResponseConverter converter;
  ObjectMapper mapper;
  List<SearchResultEntry> convertedEntries;

  @BeforeEach
  void setUp() {
    converter = new SearchResponseConverter();
    mapper = new ObjectMapper();
    String mockJson = assertDoesNotThrow(
        () -> new String(Files.readAllBytes(Paths.get("src/test/resources/users.json"))));

    JsonNode root = assertDoesNotThrow(() -> mapper.readTree(mockJson));
    JsonNode results = root.path("Result").path("Results");
    List<JsonNode> rowNodes = results.findValues("Row");

    String rowsJson = assertDoesNotThrow(() -> mapper.writeValueAsString(rowNodes));
    convertedEntries = converter.convert(rowsJson);
  }

  @Test
  void testConvert() {
    assertNotNull(convertedEntries, "Converted rows should not be null");
    assertEquals(2, convertedEntries.size(), "Should have 2 users");

    SearchResultEntry first = convertedEntries.get(0);
    assertEquals("abcc1s232", first.asUnmodifiableMap().get("DisplayName"), "");
    assertEquals("abcc1s232@cyberark.cloud.39470", first.asUnmodifiableMap().get("Username"), "");
  }

  @Test
  void testWithBlankJsonString() {
    List<SearchResultEntry> entries = converter.convert(" ");
    assertEquals(0, entries.size());
  }

  @Test
  void testWithNonJsonString() {
    assertThrows(IllegalStateException.class, () -> converter.convert("\\"));
  }

  @Test
  void testToJson() throws Exception {
    String jsonOutput = converter.toJson(convertedEntries);
    assertNotNull(jsonOutput, "JSON output should not be null");

    JsonNode outputRoot = mapper.readTree(jsonOutput);
    assertEquals(2, outputRoot.size(), "Output JSON should contain 2 entries");

    JsonNode firstOutput = outputRoot.get(0);
    assertEquals("abcc1s232", firstOutput.path("DisplayName").asText(), "");
    assertEquals("abcc1s232@cyberark.cloud.39470", firstOutput.path("Username").asText(), "");
  }
}
