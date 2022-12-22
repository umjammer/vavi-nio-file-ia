package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;


public class Search {

    private final String Url = "https://archive.org/services/search/v1/scrape";

    private final Client _client;

    public Search(Client client) {
        _client = client;
    }

    public static class ScrapeRequest {

        public String Query;
        public List<String> Sorts;
        public List<String> Fields;
        public Integer Count;
        public String Cursor;
        public boolean TotalOnly;
    }

    public static class ScrapeResponse {

        public List<ScrapeResponseItem> Items = Collections.emptyList();
        public int Count;
        public String Cursor;
        public long Total;
    }

    public static class ScrapeResponseItem {

        @JacksonXmlProperty(localName = "avg_rating")
        public int AverageRating;

        public LocalDateTime AddedDate;

        @JacksonXmlProperty(localName = "backup_location")
        public String BackupLocation;

        public String Btih;

        @JacksonXmlProperty(localName = "call_number")
        public String CallNumber;

        @JacksonXmlProperty(localName = "collection")
        @JsonAdapter(EnumerableStringDeserializer.class)
        public List<String> Collections;

        public String Contributor;
        public String Coverage;
        public String Creator;
        public String Date;

        @JsonAdapter(EnumerableStringDeserializer.class)
        @JacksonXmlProperty(localName = "description")
        public List<String> Descriptions;

        public long Downloads;

        @JacksonXmlProperty(localName = "external-identifier")
        @JsonAdapter(EnumerableStringDeserializer.class)
        public List<String> ExternalIdentifiers;

        public int FilesCount;
        public int FoldoutCount;

        @JacksonXmlProperty(localName = "format")
        @JsonAdapter(EnumerableStringDeserializer.class)
        public List<String> Formats;

        public String Genre;
        public String Identifier;

        //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
        public int ImageCount;

        public String IndexFlag;

        @JacksonXmlProperty(localName = "item_size")
        //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
        public long ItemSize;

        @JacksonXmlProperty(localName = "language")
        //@JsonAdapter(EnumerableStringConverter.class)
        public List<String> Languages;

        public String LicenseUrl;
        public String MediaType;
        public String Members;
        public String Month;
        public String Name;
        public String NoIndex;

        @JacksonXmlProperty(localName = "num_reviews")
        //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
        public int NumReviews;

        @JacksonXmlProperty(localName = "oai_updatedate")
        public List<OffsetDateTime> OaiUpdateDate;

        @JacksonXmlProperty(localName = "primary_collection")
        public String PrimaryCollection;

        public LocalDateTime PublicDate;
        public String Publisher;

        @JacksonXmlProperty(localName = "related-external-id")
        @JsonAdapter(EnumerableStringDeserializer.class)
        public List<String> RelatedExternalIds;

        @JacksonXmlProperty(localName = "reported-server")
        public String ReportedServer;

        @JacksonXmlProperty(localName = "reviewdate")
        public LocalDateTime ReviewDate;

        public String Rights;
        public String Scanner;
        public String ScanningCentre;
        public String Source;

        @JacksonXmlProperty(localName = "stripped_tags")
        public String StrippedTags;

        @JacksonXmlProperty(localName = "subject")
        @JsonAdapter(EnumerableStringDeserializer.class)
        public List<String> Subjects;

        public String Title;
        public String Type;
        public String Volume;

        //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
        public int Week;

        //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
        public int Year;
    }

    Map<String, String> ScrapeHelper(ScrapeRequest request) {
        Map<String, String> query = new HashMap<>();

        if (request.Query != null) query.put("q", request.Query);
        if (request.Fields != null) query.put("fields", String.join(",", request.Fields));
        if (request.Count != null) query.put("count", String.valueOf(request.Count));
        if (request.TotalOnly) query.put("total_only", "true");

        List<String> sorts = request.Sorts;
        if (sorts.contains("identifier") && sorts.size() > 1) {
            // if identifier is specified, it must be last
            sorts = request.Sorts.stream().filter(x -> !x.equalsIgnoreCase("identifier")).collect(Collectors.toList());
            sorts.add("identifier");
        }
        if (sorts != null) query.put("sorts", String.join(",", sorts));

        return query;
    }

    public ScrapeResponse ScrapeAsync(ScrapeRequest request) throws IOException, InterruptedException {
        Map<String, String> query = ScrapeHelper(request);
        return _client.GetAsync(Url, query, ScrapeResponse.class);
    }

    public JsonObject ScrapeAsJsonAsync(ScrapeRequest request) throws IOException, InterruptedException {
        Map<String, String> query = ScrapeHelper(request);
        return _client.GetAsync(Url, query, JsonObject.class);
    }
}