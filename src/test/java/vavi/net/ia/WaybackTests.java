package vavi.net.ia;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class WaybackTests extends Base {

    @Test
    public void IsAvailable() throws Exception {
        Wayback.IsAvailableResponse response = _client.Wayback.IsAvailable("www.bombfactory.com", null);

        assertNotNull(response);
        assertTrue(response.IsAvailable);
        assertNotNull(response.Url);
        assertNotNull(response.Timestamp);
        assertEquals(200, response.Status);
    }

    @Test
    public void IsAvailableTimestamp() throws Exception {
        Wayback.IsAvailableResponse response = _client.Wayback.IsAvailable("www.bombfactory.com", LocalDate.of(2000, 7, 4));

        assertNotNull(response);
        assertTrue(response.IsAvailable);
        assertNotNull(response.Url);
        assertEquals(2000, response.Timestamp.getYear());
        assertEquals(200, response.Status);
    }

    @Test
    public void IsNotAvailable() throws Exception {
        Wayback.IsAvailableResponse response = _client.Wayback.IsAvailable("www.bombfactory.com__", null);

        assertNotNull(response);
        assertFalse(response.IsAvailable);
    }
}