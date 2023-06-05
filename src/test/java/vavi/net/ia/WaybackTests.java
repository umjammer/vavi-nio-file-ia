package vavi.net.ia;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class WaybackTests extends Base {

    @Test
    public void search() throws Exception {
        var response1 = client.wayback.search(
                new Wayback.SearchRequest() {{
                    url = "www.experimentaltvcenter.org";
                    limit = 10;
                }});

        assertNotNull(response1);
        assertEquals(10, response1.results.size());
        assertNotNull(response1.resumeKey);

        var response2 = client.wayback.search(
                new Wayback.SearchRequest() {{
                    url = "www.experimentaltvcenter.org";
                    limit = 25;
                    resumeKey = response1.resumeKey;
                }});

        assertNotNull(response2);
        assertEquals(25, response2.results.size());
        List<LocalDateTime> tss1 = response1.results.stream().map(x -> x.timestamp).toList();
        assertEquals(25, response2.results.stream().map(x -> x.timestamp).filter(y -> !tss1.contains(y)).count());

        var response3 = client.wayback.search(
                new Wayback.SearchRequest() {{
                    url = "www.experimentaltvcenter.org";
                    limit = 25;
                    offset = 10;
                }});

        List<LocalDateTime> tss2 = response2.results.stream().map(x -> x.timestamp).toList();
        assertEquals(0, response3.results.stream().map(x -> x.timestamp).filter(y -> !tss2.contains(y)).count());
    }

    @Test
    public void searchRange() throws Exception {
        var _startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        var _endTime = LocalDateTime.of(2002, 1, 1, 0, 0);

        var response = client.wayback.search(
                new Wayback.SearchRequest() {{
                    url = "www.experimentaltvcenter.org";
                    startTime = _startTime;
                    endTime = _endTime;
                }});

        assertNotNull(response);
        assertFalse(response.results.stream().anyMatch(x -> x.timestamp.isBefore(_startTime)));
        assertFalse(response.results.stream().anyMatch(x -> x.timestamp.isAfter(_endTime)));
    }

    @Test
    public void searchRangePaging() throws Exception {
        var request = new Wayback.SearchRequest() {{
                url = "www.experimentaltvcenter.org";
                startTime = LocalDateTime.of(2000, 1, 1, 0, 0);
                endTime = LocalDateTime.of(2002, 1, 1, 0, 0);
                limit = 3;
            }};

        while (true) {
            var response = client.wayback.search(request);
            assertNotNull(response);

            if (response.resumeKey == null) break;
            request.resumeKey = response.resumeKey;
        }
    }

    @Test
    public void getNumPages() throws Exception {
        var pages = client.wayback.getNumPages("www.experimentaltvcenter.org");
        assertNotNull(pages);
    }

    @Test
    public void isAvailable() throws Exception {
        Wayback.IsAvailableResponse response = client.wayback.isAvailable("www.bombfactory.com", null);

        assertNotNull(response);
        assertTrue(response.isAvailable);
        assertNotNull(response.url);
        assertNotNull(response.timestamp);
        assertEquals(200, response.status);
    }

    @Test
    public void isAvailableTimestamp() throws Exception {
        Wayback.IsAvailableResponse response = client.wayback.isAvailable("www.bombfactory.com", LocalDateTime.of(2000, 7, 4, 0, 0));

        assertNotNull(response);
        assertTrue(response.isAvailable);
        assertNotNull(response.url);
        assertEquals(2000, response.timestamp.getYear());
        assertEquals(200, response.status);
    }

    @Test
    public void isNotAvailable() throws Exception {
        Wayback.IsAvailableResponse response = client.wayback.isAvailable("www.bombfactory.com__", null);

        assertNotNull(response);
        assertFalse(response.isAvailable);
    }
}