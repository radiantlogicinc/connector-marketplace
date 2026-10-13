package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.DATA_KEY;
import static com.radiantlogic.utility.Constants.GSON;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.radiantlogic.model.WorkdayResponse;
import java.lang.reflect.Type;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Generic JSON deserializer for {@code WorkdayResponse<T>} objects.
 *
 * <p>Ensures that the expected data field is present during deserialization
 * and converts the response into a strongly typed {@code WorkdayResponse}.</p>
 */
@Slf4j
@NoArgsConstructor
public class WorkdayResponseDeserializer<T>
    implements JsonDeserializer<WorkdayResponse<T>> {

  /**
   * Deserializes a Workday response JSON into a {@link WorkdayResponse}.
   *
   * <p>This implementation ensures that the required {@code data} field
   * is present and non-null. If the {@code data} field contains a single JSON object instead of an
   * array, it is automatically wrapped into a {@link JsonArray} to maintain a consistent response
   * structure.
   *
   * @param jsonElement JSON element to deserialize
   * @param typeOfT     Target type of the deserialized object
   * @param context     Deserialization context
   * @return Deserialized {@link WorkdayResponse} instance
   * @throws JsonParseException If the required {@code data} field is missing or null
   */
  @Override
  public WorkdayResponse<T> deserialize(
      final JsonElement jsonElement,
      final Type typeOfT,
      final JsonDeserializationContext context) {
    if (jsonElement.isJsonNull() || !jsonElement.isJsonObject()) {
      throw new JsonParseException(
          "Missing or null required field: data in Workday response");
    } else {
      JsonObject responseObj = jsonElement.getAsJsonObject();
      if (!responseObj.has(DATA_KEY)) {
        JsonArray array = new JsonArray();
        array.add(responseObj);
        responseObj.add(DATA_KEY, array);
      }
      return GSON.fromJson(responseObj, typeOfT);
    }
  }
}

