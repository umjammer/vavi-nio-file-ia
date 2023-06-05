package vavi.net.ia;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import vavi.util.Debug;


public interface JsonConverters {

    String iaDateTime = "yyyy-MM-dd HH:mm:ss";

    class ZonedDateTimeNullableConverter implements JsonDeserializer<ZonedDateTime>, JsonSerializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = json.getAsString();
            return value == null || value.isEmpty() ? null : ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(iaDateTime));
        }

        @Override
        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return context.serialize(null);
            } else {
                return context.serialize(src.toString());
            }
        }
    }

    class LocalDateTimeNullableConverter implements JsonDeserializer<LocalDateTime>, JsonSerializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String value = json.getAsString();
            var dtf = new DateTimeFormatterBuilder().appendPattern(iaDateTime).appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true).toFormatter();
            return value == null || value.isEmpty() ? null : ZonedDateTime.parse(value, dtf.withZone(ZoneId.of("UTC"))).toLocalDateTime();
        }

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return context.serialize(null);
            } else {
                return context.serialize(src.toString());
            }
        }
    }

    class LocalDateNullableConverter implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String value = json.getAsString();
            return value == null || value.isEmpty() ? null : LocalDate.parse(value);
        }

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return context.serialize(null);
            } else {
                return context.serialize(src.toString());
            }
        }
    }

    class WaybackZonedDateTimeNullableConverter implements JsonDeserializer<ZonedDateTime>, JsonSerializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String value = json.getAsString();
            return value == null || value.isEmpty() ? null : ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(Wayback.dateFormat).withZone(ZoneId.of("UTC")));
        }

        @Override
        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return context.serialize(null);
            } else {
                return context.serialize(src.format(DateTimeFormatter.ofPattern(Wayback.dateFormat)));
            }
        }
    }

    class UnixEpochDateTimeNullableConverter implements JsonDeserializer<LocalDateTime>, JsonSerializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            long unixTimeSeconds = 0;
            if (json.isJsonPrimitive()) {
                unixTimeSeconds = Long.parseLong(json.getAsString());
            } else {
                throw new IllegalArgumentException("Unexpected type " + typeOfT);
            }

            return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimeSeconds), ZoneId.of("UTC"));
        }

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return context.serialize(null);
            } else {
                return context.serialize(src.toEpochSecond(ZoneOffset.UTC));
            }
        }
    }

    class NullableStringToIntConverter implements JsonDeserializer<Integer>, JsonSerializer<Integer> {

        @Override
        public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
Debug.println(Level.FINER, "typeOfT: " + typeOfT);
            if (typeOfT == String.class) {
                var s = json.getAsString();
                if (s == null) return null;
                return Integer.parseInt(s);
            } else if (isNumber(typeOfT)) {
                return json.getAsInt();
            } else {
                if (!json.isJsonNull()) {
                    throw new IllegalArgumentException("Unexpected token type: " + typeOfT);
                } else {
                    return null;
                }
            }
        }

        boolean isNumber(Type t) {
            return t == Number.class ||
                    t == Integer.class ||
                    t == Integer.TYPE;
        }

        @Override
        public JsonElement serialize(Integer i, Type typeOfSrc, JsonSerializationContext context) {
            if (i == null) {
                return JsonNull.INSTANCE;
            } else {
                return context.serialize(String.valueOf(i));
            }
        }
    }

    class EnumerableStringConverter implements JsonDeserializer<String[]>, JsonSerializer<String[]> {

        @Override
        public String[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
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
                    throw new IllegalArgumentException("Unexpected token type: " + json);
                }
            }

            return list.toArray(String[]::new);
        }

        @Override
        public JsonElement serialize(String[] src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            } else {
                JsonArray array = new JsonArray();
                for (String s : src) {
                    array.add(context.serialize(s, String.class));
                }
                return array;
            }
        }
    }

    class NumberAdapter extends TypeAdapter<Number> {

        @Override
        public Number read(JsonReader reader) throws IOException {
            String numberString = reader.nextString();

            try {
                return Double.parseDouble(numberString);
            } catch (NumberFormatException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void write(JsonWriter writer, Number number) throws IOException {
            writer.value(number.toString());
        }
    }

    class ListConverter<T> implements JsonDeserializer<List<T>> {

        @Override
        public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
Debug.println(Level.FINER, "typeOfT: " + typeOfT);
            Type type = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
Debug.printf(Level.FINER, "List<%s>", type);
            List<T> list = new ArrayList<>();
            if (json.isJsonPrimitive()) {
                list.add(context.deserialize(json, type));
            } else if (json.isJsonArray()) {
                json.getAsJsonArray().forEach(e -> list.add(context.deserialize(e, type)));
            } else {
                if (!json.isJsonNull()) {
                    throw new IllegalArgumentException("Unexpected token type: " + json);
                }
            }

            return list;
        }
    }

    class MapConverter<T> implements JsonDeserializer<Map<String, T>> {

        @Override
        public Map<String, T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
Debug.println(Level.FINER, "typeOfT: " + typeOfT);
            Type V = ((ParameterizedType) typeOfT).getActualTypeArguments()[1];
Debug.printf(Level.FINER, "Map<String, %s>", V);
            Map<String, T> map = new HashMap<>();
            if (json.isJsonObject()) {
                json.getAsJsonObject().entrySet().forEach(e -> map.put(e.getKey(), context.deserialize(e.getValue(), V)));
            } else {
                if (!json.isJsonNull()) {
                    throw new IllegalArgumentException("Unexpected token type: " + json);
                }
            }

            return map;
        }
    }
}