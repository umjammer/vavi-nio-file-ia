package vavi.net.ia;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


class LocalOffsetDateTimeNullableDeserializer implements JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String value = json.getAsString();
        return value == null || value.isEmpty() ? null : LocalDateTime.parse(value);
    }
}

class LocalOffsetDateTimeNullableSerializer implements JsonSerializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return context.serialize(null);
        } else {
            return context.serialize(src.toString());
        }
    }
}

class LocalDateTimeNullableDeserializer implements JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String value = json.getAsString();
        return value == null || value.isEmpty() ? null : LocalDateTime.parse(value);
    }
}

class LocalDateTimeNullableSerializer implements JsonSerializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return context.serialize(null);
        } else {
            return context.serialize(src.toString());
        }
    }
}

class WaybackOffsetDateTimeNullableDeserializer implements JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String value = json.getAsString();
        return value == null || value.isEmpty() ? null : LocalDateTime.parse(value, DateTimeFormatter.ofPattern(Wayback.DateFormat));
    }
}

class WaybackOffsetDateTimeNullableSerializer implements JsonSerializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return context.serialize(null);
        } else {
            return context.serialize(src.format(DateTimeFormatter.ofPattern(Wayback.DateFormat)));
        }
    }
}

class UnixEpochDateTimeNullableDeserializer implements JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        long unixTimeSeconds = 0;
        if (typeOfT == String.class) {
            unixTimeSeconds = Long.parseLong(json.getAsString());
        } else if (typeOfT == Number.class) {
            unixTimeSeconds = json.getAsLong();
        } else {
            if (!json.isJsonNull()) {
                throw new IllegalArgumentException("Unexpected type {reader.TokenType}");
            }
        }

        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimeSeconds), ZoneId.of("UTC"));
    }
}

class UnixEpochDateTimeNullableSerializer implements JsonSerializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return context.serialize(null);
        } else {
            return context.serialize(src.toEpochSecond(ZoneOffset.UTC));
        }
    }
}

class NullableStringToIntDeserializer implements JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> list = new ArrayList<>();

        if (typeOfT == String.class) {
            var s = json.getAsString();
            if (s == null) return null;
            return Integer.parseInt(s);
        } else if (typeOfT == Number.class) {
            return json.getAsInt();
        } else {
            if (!json.isJsonNull()) {
                throw new IllegalArgumentException("Unexpected token type");
            } else {
                return null;
            }
        }
    }
}

class NullableStringToIntSerializer implements JsonSerializer<Integer> {

    @Override
    public JsonElement serialize(Integer i, Type typeOfSrc, JsonSerializationContext context) {
        if (i == null) {
            return context.serialize(null);
        } else {
            return context.serialize(String.valueOf(i));
        }
    }
}

class EnumerableStringDeserializer implements JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> list = new ArrayList<>();

        if (json.isJsonPrimitive()) {
            list.add(json.getAsString());
        } else if (json.isJsonArray()) {
            JsonArray a = json.getAsJsonArray();
            for (JsonElement e : a) {
                list.add(e.getAsString());
            }
        } else {
            if (!json.isJsonNull()) {
                throw new IllegalArgumentException("Unexpected token type");
            }
        }

        return list;
    }
}

class EnumerableStringSerializer implements JsonSerializer<List<String>> {

    @Override
    public JsonElement serialize(List<String> src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) {
            return context.serialize(null);
        } else {
            return context.serialize(src);
        }
    }
}
