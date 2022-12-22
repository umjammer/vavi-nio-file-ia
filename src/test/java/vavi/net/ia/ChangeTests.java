package vavi.net.ia;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ChangeTests extends Base {

    private static final int _pageSize = 50000;

    private static String ValidateResponse(Changes.GetResponse response, Integer countExpected/* = null*/) {
        assertNotNull(response);
        assertNotNull(response.Token);

        assertNotNull(response.Changes);

        if (countExpected != null) {
            Assertions.assertEquals(countExpected, response.Changes.size());
        }

        return response.Token;
    }

    @Test
    public void GetAsync() throws Exception {
        Changes.GetResponse response = _client.Changes.GetAsync(LocalDate.of(2021, 1, 1));
        String token = ValidateResponse(response, _pageSize);

        response = _client.Changes.GetAsync(token);
        ValidateResponse(response, _pageSize);

        response = _client.Changes.GetAsync(LocalDate.of(2021, 1, 1));
        token = ValidateResponse(response, _pageSize);

        response = _client.Changes.GetAsync(token);
        ValidateResponse(response, _pageSize);
    }

    @Test
    public void GetFromBeginningAsync() throws Exception {
        Changes.GetResponse response = _client.Changes.GetFromBeginningAsync();
        String token = ValidateResponse(response, _pageSize);

        response = _client.Changes.GetAsync(token);
        ValidateResponse(response, _pageSize);
    }

    @Test
    public void GetStartingNowAsync() throws Exception {
        Changes.GetResponse response = _client.Changes.GetStartingNowAsync();
        String token = ValidateResponse(response, 0);

        response = _client.Changes.GetAsync(token);
        ValidateResponse(response, null);
    }
}