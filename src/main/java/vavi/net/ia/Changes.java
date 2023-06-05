package vavi.net.ia;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;
import vavi.net.ia.dotnet.KeyValuePair;


public class Changes {

    private static final String url = "https://be-api.us.archive.org/changes/v1";

    private final Client client;

    public Changes(Client client) {
        this.client = client;
    }

    public static class GetResponse {
        @SerializedName("estimated_distance_from_head")
        public int estimatedDistanceFromHead;

        @SerializedName("do_sleep_before_returning")
        public boolean sleepBeforeReturning;

        public static class Change {
            public String identifier = null;
        }

        public List<Change> changes;

        @SerializedName("next_token")
        public String token;

        public List<Change> identifiers() {
            return changes.stream().filter(x -> x.identifier != null).collect(Collectors.toList());
        }
    }

    private GetResponse getHelper(String token/* = null*/, LocalDateTime startDate/*= null*/, Boolean fromBeginning/* = null*/) throws IOException, InterruptedException {
        List<KeyValuePair<String, String>> formData = new ArrayList<>();
        formData.add(new KeyValuePair<>("access", client.accessKey));
        formData.add(new KeyValuePair<>("secret", client.secretKey));

        if (token != null) formData.add(new KeyValuePair<>("token", token));

        if (startDate != null) {
            formData.add(new KeyValuePair<>("start_date", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))));
        } else if (fromBeginning != null && fromBeginning) {
            formData.add(new KeyValuePair<>("start_date", "0"));
        }

        String form = formData.stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        var response = client.send("POST", url, "application/x-www-form-urlencoded", HttpRequest.BodyPublishers.ofString(form), GetResponse.class);
        if (response == null) throw new IllegalStateException("null response from server");

        return response;
    }

    public GetResponse getFromBeginning() throws IOException, InterruptedException {
        return getHelper(null, null, true);
    }

    public GetResponse getStartingNow() throws IOException, InterruptedException {
        return getHelper(null, null, null);
    }

    public GetResponse get(String token) throws IOException, InterruptedException {
        return getHelper(token, null, null);
    }

    public GetResponse get(LocalDateTime startDate) throws IOException, InterruptedException {
        return getHelper(null, startDate, null);
    }

    public GetResponse get(LocalDate startDate) throws IOException, InterruptedException {
        return getHelper(null, LocalDateTime.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth(), 0, 0), null);
    }
}