package it.tidal.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import it.tidal.config.utils.Hour;
import java.lang.reflect.Type;

public class HourAdapter implements JsonSerializer<Hour>, JsonDeserializer<Hour> {

    @Override
    public JsonElement serialize(Hour h, Type type, JsonSerializationContext context) {

        return new JsonPrimitive(h.toString());
    }

    @Override
    public Hour deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {

        try {
            return Hour.fromString(json.getAsString());
        } catch (Exception ex) {
            throw new JsonParseException(ex);
        }
    }
}
