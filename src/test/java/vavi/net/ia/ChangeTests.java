package vavi.net.ia;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ChangeTests extends Base {

    private static final int pageSize = 50000;

    private static String validateResponse(Changes.GetResponse response, Integer countExpected/* = null*/) {
        assertNotNull(response);
        assertNotNull(response.token);

        assertNotNull(response.changes);

        if (countExpected != null) {
            Assertions.assertEquals(countExpected, response.changes.size());
        }

        return response.token;
    }

    @Test
    public void get() throws Exception {
        Changes.GetResponse response = client.changes.get(LocalDateTime.of(2021, 1, 1, 0, 0));
        String token = validateResponse(response, pageSize);

        response = client.changes.get(token);
        validateResponse(response, pageSize);

        response = client.changes.get(LocalDate.of(2021, 1, 1));
        token = validateResponse(response, pageSize);

        response = client.changes.get(token);
        validateResponse(response, pageSize);
    }

    @Test
    public void getFromBeginning() throws Exception {
        Changes.GetResponse response = client.changes.getFromBeginning();
        String token = validateResponse(response, pageSize);

        response = client.changes.get(token);
        validateResponse(response, pageSize);
    }

    @Test
    public void getStartingNow() throws Exception {
        Changes.GetResponse response = client.changes.getStartingNow();
        String token = validateResponse(response, 0);

        response = client.changes.get(token);
        validateResponse(response, null);
    }
}