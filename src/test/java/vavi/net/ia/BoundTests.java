package vavi.net.ia;

import java.time.LocalDate;
import java.util.List;

import com.google.gson.annotations.JsonAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class BoundTests extends Base {

    @Test
    public void enumerableStringNullableConverter() {
        Metadata.ReadResponse response = new Metadata.ReadResponse();
        var json = Client.gson.toJson(response);

        Metadata.ReadResponse test = Client.gson.fromJson(json, Metadata.ReadResponse.class);

        assertNotNull(test);
        assertNull(test.workableServers);

        var test2 = Client.gson.fromJson("{\"workable_servers\":\"1\"}", Metadata.ReadResponse.class);

        assertEquals(1, test2.workableServers.size());
        response.workableServers = List.of("1", "2");

        json = Client.gson.toJson(response);
        var test3 = Client.gson.fromJson(json, Metadata.ReadResponse.class);
        assertEquals(2, test3.workableServers.size());
    }

    public static class TestLocalDate {

        @JsonAdapter(JsonConverters.LocalDateNullableConverter.class)
        public LocalDate testDate;
    }

    @Test
    public void localDateConverter() {
        LocalDate testDate = LocalDate.of(2001, 1, 25);

        TestLocalDate response = new TestLocalDate();
        response.testDate = testDate;
        var json = Client.gson.toJson(response);

        var test = Client.gson.fromJson(json, TestLocalDate.class);
        assertNotNull(test);
        assertEquals(testDate, test.testDate);

        json = "{ \"testDate\" : null }";
        test = Client.gson.fromJson(json, TestLocalDate.class);
        assertNotNull(test);
        assertNull(test.testDate);
    }
}