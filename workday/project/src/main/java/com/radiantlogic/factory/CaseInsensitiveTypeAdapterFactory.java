package com.radiantlogic.factory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Custom Gson TypeAdapterFactory that enables case-insensitive JSON key matching.
 *
 * <p>This factory normalizes JSON keys to match Java field names in a case-insensitive
 * manner, supporting variations like "userName", "username", and "UserName".</p>
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class CaseInsensitiveTypeAdapterFactory implements TypeAdapterFactory {

  /**
   * Creates a type adapter for the specified type using the provided Gson instance.
   *
   * <p>This method is invoked by Gson when it needs to serialize or deserialize
   * objects of the given type. It allows custom handling of JSON conversion
   * logic for specific types.
   *
   * @param gson the Gson instance used for JSON processing
   * @param type the type token representing the type for which the adapter is created
   * @param <T> the type handled by the type adapter
   * @return a custom TypeAdapter instance for the specified type, or null if the
   *         default Gson adapter should be used
   */
  @Override
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.UseConcurrentHashMap"})
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
    TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

    // Build map: "report_entry" -> "Report_Entry", "username" -> "userName"
    Map<String, String> keyMap = new HashMap<>();
    for (Class<?> c = type.getRawType(); c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        SerializedName serializedName = f.getAnnotation(SerializedName.class);
        String actual = serializedName != null ? serializedName.value() : f.getName();
        keyMap.put(actual.toLowerCase(Locale.ROOT), actual);
      }
    }

    return new TypeAdapter<T>() {
      @Override
      public void write(final JsonWriter out, final T value) throws IOException {
        delegate.write(out, value);
      }

      @Override
      @SuppressWarnings("PMD.ShortVariable")
      public T read(final JsonReader in) throws IOException {
        JsonElement tree = elementAdapter.read(in);
        return delegate.fromJsonTree(tree.isJsonObject() ? normalize(tree) : tree);
      }

      @SuppressWarnings("PMD.ShortVariable")
      private JsonElement normalize(final JsonElement el) {
        if (el.isJsonObject()) {
          JsonObject out = new JsonObject();
          el.getAsJsonObject().entrySet().forEach(e -> {
            String lower = e.getKey().toLowerCase(Locale.ROOT);
            out.add(keyMap.getOrDefault(lower, lower), normalize(e.getValue()));
          });
          return out;
        }
        if (el.isJsonArray()) {
          JsonArray out = new JsonArray();
          el.getAsJsonArray().forEach(e -> out.add(normalize(e)));
          return out;
        }
        return el;
      }
    };
  }
}