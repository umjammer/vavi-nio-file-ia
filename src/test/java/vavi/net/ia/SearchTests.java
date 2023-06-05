package vavi.net.ia;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SearchTests extends Base {

    @Test
    public void scrape() throws Exception {
        var request = new Search.ScrapeRequest() {{
            query = "scanimate";
            fields = List.of("identifier", "title", "description");
            sorts = List.of("title");
        }};

        var response = client.search.scrape(request);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.count);
        Assertions.assertNotNull(response.total);

        Assertions.assertEquals((long) response.count, response.total);
        Assertions.assertNull(response.cursor);

        Assertions.assertEquals(response.count, response.items.size());

        var json = client.search.scrapeAsJson(request);
        var count = json.get("count").getAsInt();
        Assertions.assertEquals(response.count, count);
    }

    @Test
    public void test() throws Exception {
        var request = new Search.ScrapeRequest() {{
            query = "@vavivavi";
            fields = List.of("identifier", "title", "date", "item_size");
            sorts = List.of("title");
        }};

        var response = client.search.scrape(request);
        response.items.forEach(i -> {
            System.err.printf("%20s %20s %8s %s%n", abbr(i.identifier, 20), abbr(i.title, 20), i.itemSize, i.date);
        });
    }

    String abbr(String s, int l) {
        if (s.length() > 3 && s.length() > l) {
            return s.substring(0, s.length() - 3) + "...";
        } else {
            return s;
        }
    }
}
