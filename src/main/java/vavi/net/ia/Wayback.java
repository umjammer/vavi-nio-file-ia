package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;


public class Wayback {

    private static final String url = "https://archive.org/wayback/available";
    private static final String cdxUrl = "https://web.archive.org/cdx/search/cdx";

    static final String dateFormat = "yyyyMMddHHmmss";

    private final Client client;

    public Wayback(Client client) {
        this.client = client;
    }

    public static class IsAvailableResponse {

        @SerializedName("available")
        public boolean isAvailable;

        public String url;

        @JsonAdapter(JsonConverters.WaybackZonedDateTimeNullableConverter.class)
        public ZonedDateTime timestamp;

        @JsonAdapter(JsonConverters.NullableStringToIntConverter.class)
        public int status;
    }

    static class WaybackResponse {

        @SerializedName("archived_snapshots")
        public ArchivedSnapshots_ archivedSnapshots;

        static class ArchivedSnapshots_ {

            @SerializedName("closest")
            public IsAvailableResponse isAvailableResponse;
        }
    }

    public IsAvailableResponse isAvailable(String url, LocalDateTime timestamp/* = null*/) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("url", url);
        if (timestamp != null) query.put("timestamp", timestamp.format(DateTimeFormatter.ofPattern(dateFormat)));

        WaybackResponse response = client.get(Wayback.url, query, WaybackResponse.class);
        IsAvailableResponse b = response.archivedSnapshots.isAvailableResponse;
        return b != null ? b : new IsAvailableResponse();
    }

    static class SearchRequest {

        public String url;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String matchType;
        public String collapse;
        public Integer limit;
        public Integer offset;
        public Integer page;
        public Integer pageSize;
        public boolean fastLatest;
        public String resumeKey;

        private Map<String, String> toQuery() {
            if (url == null) throw new IllegalArgumentException("url is required");
            Map<String, String> query = new HashMap<>();
            query.put("url", url);
            query.put("showResumeKey", "true");

            if (startTime != null) query.put("from", startTime.format(DateTimeFormatter.ofPattern(dateFormat)));
            if (endTime != null) query.put("to", endTime.format(DateTimeFormatter.ofPattern(dateFormat)));
            if (matchType != null) query.put("matchType", matchType);
            if (collapse != null) query.put("collapse", collapse);
            if (limit != null) query.put("limit", String.valueOf(limit));
            if (offset != null) query.put("offset", String.valueOf(offset));
            if (page != null) query.put("page", String.valueOf(page));
            if (pageSize != null) query.put("pageSize", String.valueOf(pageSize));
            if (fastLatest) query.put("fastLatest", "true");
            if (resumeKey != null) query.put("resumeKey", resumeKey);

            return query;
        }
    }

    static class SearchResponse {

        public List<CdxResponse> results = new ArrayList<>();
        public String resumeKey;

        static class CdxResponse {

            public String urlKey = null;
            public LocalDateTime timestamp;
            public String original = null;
            public String mimeType = null;
            public int statusCode;
            public String digest = null;
            public long length;
        }
    }

    public SearchResponse search(SearchRequest request) throws IOException, InterruptedException {
        var result = client.get(cdxUrl, request.toQuery(), String.class);
        var response = new SearchResponse();

        boolean lastLine = false;
        for (var line : result.split("\n")) {
            if (line.length() == 0) {
                lastLine = true;
                continue;
            }

            if (lastLine) {
                response.resumeKey = line;
                break;
            }

            var fields = line.split(" ", 8);
            if (fields.length != 7) throw new IllegalStateException("Unexpected number of fields returned from server");

            var cdxResponse = new SearchResponse.CdxResponse();
            cdxResponse.urlKey = fields[0];
            cdxResponse.timestamp = LocalDateTime.parse(fields[1], DateTimeFormatter.ofPattern(dateFormat));
            cdxResponse.original = fields[2];
            cdxResponse.mimeType = fields[3];
            cdxResponse.digest = fields[5];
            cdxResponse.length = Long.parseLong(fields[6]);

            try {
                var statusCode = Integer.parseInt(fields[4]);
                cdxResponse.statusCode = statusCode; // can be "-"
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            response.results.add(cdxResponse);
        }

        return response;
    }

    public Integer getNumPages(String url) throws IOException, InterruptedException {
        var sr = new SearchRequest();
        sr.url = url;
        var query = sr.toQuery();
        query.put("showNumPages", "true");

        var response = client.get(cdxUrl, query, String.class).trim();
        return Integer.parseInt(response);
    }
}