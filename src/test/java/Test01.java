/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vavi.net.ia.Client;
import vavi.net.ia.Search;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * gson sandbox.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-06-06 nsano initial version <br>
 */
public class Test01 {

    static class A {

        static class B {
            int b;
        }

        List<B> bs;
    }

    static class MyAdapter<T> implements JsonDeserializer<List<T>> {

        @Override
        public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
Debug.println("typeOfT: " + typeOfT);
            List<T> l = new ArrayList<>();
            if (json.isJsonArray()) {
                json.getAsJsonArray().asList().forEach(j -> {
                    l.add(context.deserialize(j, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]));
                });
            }
            return l;
        }
    }

    @Test
    void test01() throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(new TypeToken<List<A.B>>(){}.getType(), new MyAdapter<A.B>())
                .create();
        A a = gson.fromJson("{\"bs\": [{\"b\": 1}, {\"b\": 2}, {\"b\": 3}]}", A.class);
        assertEquals(3, a.bs.size());
        assertInstanceOf(A.B.class, a.bs.get(2));
        assertEquals(1, a.bs.get(0).b);
        assertEquals(2, a.bs.get(1).b);
        assertEquals(3, a.bs.get(2).b);
    }

    @Test
    void test2() throws Exception {
        Type type = new TypeToken<List<Integer>>(){}.getType();
        Type gt = ((ParameterizedType) type).getActualTypeArguments()[0];
        assertEquals(Integer.class, gt);
    }

    @Test
    void test3() throws Exception {
        String date = "2018-02-17 09:26:17.135264";
        String localDateTime = "yyyy-MM-dd HH:mm:ss.SSSSSS";
        LocalDateTime ldt = ZonedDateTime.parse(date, DateTimeFormatter.ofPattern(localDateTime).withZone(ZoneId.of("UTC"))).toLocalDateTime();
        assertEquals(date, ldt.format(DateTimeFormatter.ofPattern(localDateTime)));
    }
}
