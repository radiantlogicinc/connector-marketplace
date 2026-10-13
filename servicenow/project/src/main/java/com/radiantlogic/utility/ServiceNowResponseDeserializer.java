package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.GSON;
import static com.radiantlogic.utility.Constants.RESULT_KEY;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.radiantlogic.model.ServiceNowResponse;
import java.lang.reflect.Type;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic deserializer that validates presence of result field for any
 * {@code ServiceNowResponse<T>}.
 */
@Slf4j
@NoArgsConstructor
public class ServiceNowResponseDeserializer<T>
    implements JsonDeserializer<ServiceNowResponse<T>> {

  /**
   * Deserializes a ServiceNow response JSON into a {@link ServiceNowResponse}.
   *
   * <p>This implementation ensures that the required {@code result} field
   * is present and non-null. If the {@code result} field contains a single JSON object instead of
   * an array, it is automatically wrapped into a {@link JsonArray} to maintain a consistent
   * response structure.
   *
   * @param jsonElement JSON element to deserialize
   * @param typeOfT     Target type of the deserialized object
   * @param context     Deserialization context
   * @return Deserialized {@link ServiceNowResponse} instance
   * @throws JsonParseException If the required {@code result} field is missing or null
   */
  @Override
  public ServiceNowResponse<T> deserialize(
      final JsonElement jsonElement,
      final Type typeOfT,
      final JsonDeserializationContext context) {
    JsonObject responseObj = jsonElement.getAsJsonObject();
    if (!responseObj.has(RESULT_KEY) || responseObj.get(RESULT_KEY).isJsonNull()) {
      throw new JsonParseException(
          "Missing or null required field: result in ServiceNow response");
    }
    JsonElement resultElement = responseObj.get(RESULT_KEY);
    if (!resultElement.isJsonArray()) {
      JsonArray array = new JsonArray();
      array.add(resultElement);
      responseObj.add(RESULT_KEY, array);
    }
    return GSON.fromJson(responseObj, typeOfT);
  }
}
