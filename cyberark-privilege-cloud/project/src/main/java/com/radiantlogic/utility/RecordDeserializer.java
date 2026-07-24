package com.radiantlogic.utility;

import static com.radiantlogic.utility.Constants.PMD_KEY;
import static com.radiantlogic.utility.Constants.ROW_KEY_NAME;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import lombok.NoArgsConstructor;

/**
 * Class to create a custom JSON deserializer for parsing Record class.
 */
@NoArgsConstructor
public class RecordDeserializer implements JsonDeserializer<Record> {

  @Override
  @SuppressWarnings(PMD_KEY)
  public Record deserialize(final JsonElement jsonElement,
                            final Type typeOfT,
                            final JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject response = jsonElement.getAsJsonObject();
    if (!response.has(ROW_KEY_NAME) || response.get(ROW_KEY_NAME).isJsonNull()) {
      throw new JsonParseException("Field 'Row' must not be null or is missing");
    }
    Record record = new Record();
    RecordRow recordRow =
        context.deserialize(response.get(ROW_KEY_NAME), RecordRow.class);
    record.setRecordRow(recordRow);
    return record;
  }
}