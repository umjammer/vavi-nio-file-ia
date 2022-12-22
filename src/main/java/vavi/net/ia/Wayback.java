package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.annotations.JsonAdapter;


public class Wayback {

    private static final String Url = "https://archive.org/wayback/available";
    private static final String CdxUrl = "https://web.archive.org/cdx/search/cdx";

    static final String DateFormat = "yyyyMMddHHmmss";

    private final Client _client;

    public Wayback(Client client) {
        _client = client;
    }

    public static class IsAvailableResponse {

        @JacksonXmlProperty(localName = "available")
        public boolean IsAvailable;

        public String Url;

        @JsonAdapter(WaybackOffsetDateTimeNullableDeserializer.class)
        public OffsetDateTime Timestamp;

        @JsonAdapter(NullableStringToIntDeserializer.class)
        public int Status;
    }

    static class WaybackResponse {

        @JacksonXmlProperty(localName = "archived_snapshots")
        public ArchivedSnapshots_ ArchivedSnapshots;

        static class ArchivedSnapshots_ {

            @JacksonXmlProperty(localName = "closest")
            public IsAvailableResponse IsAvailableResponse;
        }
    }

    public IsAvailableResponse IsAvailable(String url, LocalDate timestamp/* = null*/) throws Exception {
        Map<String, String> query = new HashMap<>();
        query.put("url", url);
        if (timestamp != null) query.put("timestamp", timestamp.toString());

        WaybackResponse response = _client.GetAsync(Url, query, WaybackResponse.class);
        IsAvailableResponse b = response.ArchivedSnapshots.IsAvailableResponse;
        return b != null ? b : new IsAvailableResponse();
    }

    static class SearchRequest {

        public String Url;
        public LocalDateTime StartTime;
        public LocalDateTime EndTime;
        public String MatchType;
        public String Collapse;
        public Integer Limit;
        public Integer Offset;
        public Integer Page;
        public Integer PageSize;
        public boolean FastLatest;
        public String ResumeKey;

        private Map<String, String> ToQuery() {
            if (Url == null) throw new IllegalArgumentException("Url is required");
            Map<String, String> query = new HashMap<>();
            query.put("url", Url);
            query.put("showResumeKey", "true");

            if (StartTime != null) query.put("from", StartTime.format(DateTimeFormatter.ofPattern(DateFormat)));
            if (EndTime != null) query.put("to", EndTime.format(DateTimeFormatter.ofPattern(DateFormat)));
            if (MatchType != null) query.put("matchType", MatchType);
            if (Collapse != null) query.put("collapse", Collapse);
            if (Limit != null) query.put("limit", String.valueOf(Limit));
            if (Offset != null) query.put("offset", String.valueOf(Offset));
            if (Page != null) query.put("page", String.valueOf(Page));
            if (PageSize != null) query.put("pageSize", String.valueOf(PageSize));
            if (FastLatest) query.put("fastLatest", "true");
            if (ResumeKey != null) query.put("resumeKey", ResumeKey);

            return query;
        }
    }

    static class SearchResponse {

        public List<CdxResponse> Results = new ArrayList<>();
        public String ResumeKey;

        static class CdxResponse {

            public String UrlKey = null;
            public LocalDateTime Timestamp;
            public String Original = null;
            public String MimeType = null;
            public int StatusCode;
            public String Digest = null;
            public long Length;
        }
    }

    public SearchResponse SearchAsync(SearchRequest request) throws IOException, InterruptedException {
        var result = _client.<String>GetAsync(CdxUrl, request.ToQuery(), String.class);
        var response = new SearchResponse();

        boolean lastLine = false;
        for (var line : result.split("\n")) {
            if (line.length() == 0) {
                lastLine = true;
                continue;
            }

            if (lastLine) {
                response.ResumeKey = line;
                break;
            }

            var fields = line.split(" ", 8);
            if (fields.length != 7) throw new IllegalStateException("Unexpected number of fields returned from server");

            var cdxResponse = new SearchResponse.CdxResponse();
            cdxResponse.UrlKey = fields[0];
            cdxResponse.Timestamp = LocalDateTime.parse(fields[1], DateTimeFormatter.ofPattern(DateFormat));
            cdxResponse.Original = fields[2];
            cdxResponse.MimeType = fields[3];
            cdxResponse.Digest = fields[5];
            cdxResponse.Length = Long.parseLong(fields[6]);

            try {
                var statusCode = Integer.parseInt(fields[4]);
                cdxResponse.StatusCode = statusCode; // can be "-"
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            response.Results.add(cdxResponse);
        }

        return response;
    }

    public Integer GetNumPagesAsync(String url) throws IOException, InterruptedException {
        var sr = new SearchRequest();
        sr.Url = url;
        var query = sr.ToQuery();
        query.put("showNumPages", "true");

        var response = _client.GetAsync(CdxUrl, query, String.class);
        return Integer.parseInt(response);
    }
}