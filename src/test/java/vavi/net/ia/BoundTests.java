package vavi.net.ia;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class BoundTests extends Base {

    @Test
    public void EnumerableStringNullableConverter() {
        Metadata.ReadResponse response = new Metadata.ReadResponse();
        var json = Client._json.toJson(response);

        try (Metadata.ReadResponse test = Client._json.fromJson(json, Metadata.ReadResponse.class)) {

            Assertions.assertNotNull(test);
            Assertions.assertNull(test.WorkableServers);

            try (var test2 = Client._json.fromJson("{\"workable_servers\":\"1\"}", Metadata.ReadResponse.class)) {

                Assertions.assertEquals(1, test2.WorkableServers.length);
            }
            response.WorkableServers = new String[] {"1", "2"};

            json = Client._json.toJson(response);
            try (var test3 = Client._json.fromJson(json, Metadata.ReadResponse.class)) {
                Assertions.assertEquals(2, test3.WorkableServers.length);
            }
        }
    }

    public static class TestLocalDate {

        public LocalDate TestDate;
    }

    @Test
    public void LocalDateConverter() {
        LocalDate testDate = LocalDate.of(2001, 1, 25);

        TestLocalDate response = new TestLocalDate();
        response.TestDate = testDate;
        var json = Client._json.toJson(response);

        var test = Client._json.fromJson(json, TestLocalDate.class);
        Assertions.assertNotNull(test);
        Assertions.assertEquals(testDate, test.TestDate);

        json = "{ \"TestDate\" : null }";
        test = Client._json.fromJson(json, TestLocalDate.class);
        Assertions.assertNotNull(test);
        Assertions.assertNull(test.TestDate);
    }
}