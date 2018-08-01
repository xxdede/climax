package it.tidal.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import it.tidal.config.utils.Utility;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime dt, Type typeOfSrc, JsonSerializationContext context) {

        return new JsonPrimitive(dt.format(Utility.basicDateTimeFormatter));
    }

    @Override
    public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {

        try {
            return LocalDateTime.parse(json.getAsString(), Utility.basicDateTimeFormatter);
        } catch (DateTimeParseException ex) {
            throw new JsonParseException(ex);
        }
    }
}
