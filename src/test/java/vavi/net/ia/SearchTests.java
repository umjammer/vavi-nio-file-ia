package vavi.net.ia;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SearchTests extends Base {

    @Test
    public void ScrapeAsync() throws Exception {
        var request = new Search.ScrapeRequest() {{
            Query = "scanimate";
            Fields = List.of("identifier", "title", "description");
            Sorts = List.of("title");
        }};

        var response = _client.Search.ScrapeAsync(request);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.Count);
        Assertions.assertNotNull(response.Total);

        Assertions.assertEquals(response.Count, response.Total);
        Assertions.assertNull(response.Cursor);

        Assertions.assertEquals(response.Count, response.Items.size());

        var json = _client.Search.ScrapeAsJsonAsync(request);
        var count = json.get("count").getAsInt();
        Assertions.assertEquals(response.Count, count);
    }
}
