package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.MEMBERS_KEY;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import lombok.NoArgsConstructor;

/**
 * Class to create a custom JSON serializer for RecordRow class for update operations.
 */
@NoArgsConstructor
public class RecordRowUpdateSerializer
    implements JsonSerializer<RecordRow> {

  /**
   * GSON used to serialize Json object.
   */
  private static final Gson GSON = new Gson();

  @Override
  public JsonElement serialize(
      final RecordRow src,
      final Type typeOfSrc,
      final JsonSerializationContext context) {
    JsonObject jsonObject =
        GSON.toJsonTree(src).getAsJsonObject();
    if (src.getMembers() == null || src.getMembers().isEmpty()) {
      jsonObject.remove(MEMBERS_KEY);
    }
    return jsonObject;
  }
}
