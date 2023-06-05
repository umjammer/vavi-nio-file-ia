package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;


public class Search {

    private static final String url = "https://archive.org/services/search/v1/scrape";

    private final Client client;

    public Search(Client client) {
        this.client = client;
    }

    public static class ScrapeRequest {

        public String query;
        public List<String> sorts;
        public List<String> fields;
        public Integer count;
        public String cursor;
        public boolean totalOnly;
    }

    public static class ScrapeResponse {

        public List<ScrapeResponseItem> items = Collections.emptyList();
        public Integer count;
        public String cursor;
        public Long total;
    }

    public static class ScrapeResponseItem {

        @SerializedName("avg_rating")
        public int averageRating;

        public LocalDateTime addedDate;

        @SerializedName("backup_location")
        public String backupLocation;

        public String btih;

        @SerializedName("call_number")
        public String callNumber;

        @SerializedName("collection")
        public List<String> collections;

        public String contributor;
        public String coverage;
        public String creator;
        public String date;

        @SerializedName("description")
        public List<String> descriptions;

        public long downloads;

        @SerializedName("external-identifier")
        public List<String> externalIdentifiers;

        public int filesCount;
        public int foldoutCount;

        @SerializedName("format")
        public List<String> formats;

        public String genre;
        public String identifier;

        //@JsonAdapter(JsonConverters.NumberAdapter.class)
        public int imageCount;

        public String indexFlag;

        @SerializedName("item_size")
        //@JsonAdapter(JsonConverters.NumberAdapter.class)
        public long itemSize;

        @SerializedName("language")
        public List<String> languages;

        public String licenseUrl;
        public String mediaType;
        public String members;
        public String month;
        public String name;
        public String noIndex;

        @SerializedName("num_reviews")
        //@JsonAdapter(JsonConverters.NumberAdapter.class)
        public int numReviews;

        @SerializedName("oai_updatedate")
        public List<ZonedDateTime> oaiUpdateDate;

        @SerializedName("primary_collection")
        public String primaryCollection;

        public LocalDateTime publicDate;
        public String publisher;

        @SerializedName("related-external-id")
        public List<String> relatedExternalIds;

        @SerializedName("reported-server")
        public String reportedServer;

        @SerializedName("reviewdate")
        public LocalDateTime reviewDate;

        public String rights;
        public String scanner;
        public String scanningCentre;
        public String source;

        @SerializedName("stripped_tags")
        public String strippedTags;

        @SerializedName("subject")
        public List<String> subjects;

        public String title;
        public String type;
        public String volume;

        //@JsonAdapter(JsonConverters.NumberAdapter.class)
        public int week;

        //@JsonAdapter(JsonConverters.NumberAdapter.class)
        public int year;
    }

    Map<String, String> scrapeHelper(ScrapeRequest request) {
        Map<String, String> query = new HashMap<>();

        if (request.query != null) query.put("q", request.query);
        if (request.fields != null) query.put("fields", String.join(",", request.fields));
        if (request.count != null) query.put("count", String.valueOf(request.count));
        if (request.totalOnly) query.put("total_only", "true");

        List<String> sorts = request.sorts;
        if (sorts.contains("identifier") && sorts.size() > 1) {
            // if identifier is specified, it must be last
            sorts = request.sorts.stream().filter(x -> !x.equalsIgnoreCase("identifier")).collect(Collectors.toList());
            sorts.add("identifier");
        }
        if (sorts != null) query.put("sorts", String.join(",", sorts));

        return query;
    }

    public ScrapeResponse scrape(ScrapeRequest request) throws IOException, InterruptedException {
        Map<String, String> query = scrapeHelper(request);
        return client.get(url, query, ScrapeResponse.class);
    }

    public JsonObject scrapeAsJson(ScrapeRequest request) throws IOException, InterruptedException {
        Map<String, String> query = scrapeHelper(request);
        return client.get(url, query, JsonObject.class);
    }
}