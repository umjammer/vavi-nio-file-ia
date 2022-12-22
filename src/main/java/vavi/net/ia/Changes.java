package vavi.net.ia;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import vavi.net.ia.dotnet.KeyValuePair;


public class Changes {

    private static final String Url = "https://be-api.us.archive.org/changes/v1";

    private final Client _client;

    public Changes(Client client) {
        _client = client;
    }

    public static class GetResponse {
        @JacksonXmlProperty(localName = "estimated_distance_from_head")
        public int EstimatedDistanceFromHead;

        @JacksonXmlProperty(localName = "do_sleep_before_returning")
        public boolean SleepBeforeReturning;

        public static class Change {
            public String Identifier = null;
        }

        public List<Change> Changes;

        @JacksonXmlProperty(localName = "next_token")
        public String Token;

        public List<Change> Identifiers() {
            return Changes.stream().filter(x -> x.Identifier != null).collect(Collectors.toList());
        }
    }

    private GetResponse GetHelperAsync(String token/* = null*/, LocalDateTime startDate/*= null*/, Boolean fromBeginning/* = null*/) throws IOException, InterruptedException {
        List<KeyValuePair<String, String>> formData = new ArrayList<>();
        formData.add(new KeyValuePair<>("access", _client.AccessKey));
        formData.add(new KeyValuePair<>("secret", _client.SecretKey));

        if (token != null) formData.add(new KeyValuePair<>("token", token));

        if (startDate != null) {
            formData.add(new KeyValuePair<>("start_date", "{startDate:yyyyMMdd}"));
        } else if (fromBeginning) {
            formData.add(new KeyValuePair<>("start_date", "0"));
        }

        String form = formData.stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        var response = _client.<GetResponse>SendAsync("POST", Url, "application/x-www-form-urlencoded", HttpRequest.BodyPublishers.ofString(form), GetResponse.class);
        if (response == null) throw new IllegalStateException("null response from server");

        return response;
    }

    public GetResponse GetFromBeginningAsync() throws IOException, InterruptedException {
        return GetHelperAsync(null, null, /*fromBeginning:*/ true);
    }

    public GetResponse GetStartingNowAsync() throws IOException, InterruptedException {
        return GetHelperAsync(null, null, null);
    }

    public GetResponse GetAsync(String token) throws IOException, InterruptedException {
        return GetHelperAsync(token, null, null);
    }

    public GetResponse GetAsync(LocalDateTime startDate) throws IOException, InterruptedException {
        return GetHelperAsync(null, /*startDate:*/ startDate, null);
    }

//#if NET
    public GetResponse GetAsync(LocalDate startDate) {
        return GetAsync(LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth()));
    }
//#endif
}