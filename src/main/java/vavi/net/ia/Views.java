package vavi.net.ia;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;


/**
 * @see "https://archive.org/developers/views_api.html"
 */
public class Views {

    private static final String url = "https://be-api.us.archive.org/views/v1";

    private static <T extends Temporal> String detailsUrl(String type, String id, T startDate, T endDate) {
        return String.format("%1$s/detail/%2$s/%3$s/%4$tY%4$tm%4$td/%5$tY%5$tm%5$td", url, type, URLEncoder.encode(id, StandardCharsets.UTF_8), startDate, endDate);
    }

    private final Client client;

    public Views(Client client) {
        this.client = client;
    }

    public static class Summary {
        @SerializedName("have_data")
        public boolean hasData;

        @SerializedName("last_7day")
        public Long last7Days;

        @SerializedName("last_30day")
        public Long last30Days;

        @SerializedName("all_time")
        public Long allTime;

        public SummaryDetail detail;
    }

    public static class SummaryDetail {
        @SerializedName("pre_20170101_total")
        public Long pre2017Total;

        @SerializedName("non_robot")
        public SummaryDetailStats nonRobot;

        public SummaryDetailStats robot;
        public SummaryDetailStats unrecognized;
        public SummaryDetailStats pre2017;
    }

    public static class SummaryDetailStats {
        @SerializedName("per_day")
        public List<Long> perDay = new ArrayList<>();

        @SerializedName("previous_days_total")
        public Long previousDaysTotal;

        @SerializedName("sum_per_day_data")
        public Long sumPerDay;
    }

    /** @throws NoSuchElementException when not found */
    public Summary getItemSummary(String identifier, boolean legacy/* = false*/) throws IOException, InterruptedException {
        Map<String, Summary> summaries = getItemSummary(new String[] {identifier}, legacy);
        if (summaries.size() == 0) throw new NoSuchElementException("identifier not found: " + identifier);
        return summaries.values().stream().findFirst().get();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Summary> getItemSummary(String[] identifiers, boolean legacy/* = false*/) throws IOException, InterruptedException {
        String api = legacy ? "legacy_counts" : "short";
        return client.get(String.format("%s/%s/%s", url, api, String.join(",", identifiers)), null, Map.class, new TypeToken<Map<String, Summary>>(){}.getType());
    }

    public static class SummaryPerDay<T> {
        public List<T> days = new ArrayList<>();
        public Map<String, Summary> ids = new HashMap<>();
    }

    public <T> SummaryPerDay<T> getItemSummaryPerDay(String identifier) throws IOException, InterruptedException {
        return this.getItemSummaryPerDay(new String[] {identifier});
    }

    @SuppressWarnings("unchecked")
    public <T> SummaryPerDay<T> getItemSummaryPerDay(String[] identifiers) throws IOException, InterruptedException {
        return client.get(url + "/long/" + String.join(",", identifiers), null, SummaryPerDay.class);
    }

    public static class Details<T> {
        @SerializedName("counts_geo")
        public List<GeoCount> counts = new ArrayList<>();
        public List<T> days = new ArrayList<>();

        public List<Referer_> referers = new ArrayList<>();

        public static class GeoCount {
            @SerializedName("count_kind")
            public String countKind;

            public String country;

            @SerializedName("geo_country")
            public String geoCountry;

            @SerializedName("geo_state")
            public String geoState;

            @SerializedName("lat")
            public Float latitude;

            @SerializedName("lng")
            public Float longitude;

            public String state;

            @SerializedName("sum_count_value")
            public Long count;

            @SerializedName("ua_kind")
            public String kind;
        }

        public static class Referer_ {
            public String referer;
            public Long score;

            @SerializedName("ua_kind")
            public String kind;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Temporal> Details<T> getItemDetails(String identifier, T startDate, T endDate) throws IOException, InterruptedException {
        return client.get(detailsUrl("item", identifier, startDate, endDate), null, Details.class);
    }

    @SuppressWarnings("unchecked")
    public <T extends Temporal> Details<T> getCollectionDetails(String collection, T startDate, T endDate) throws IOException, InterruptedException {
        return client.get(detailsUrl("collection", collection, startDate, endDate), null, Details.class);
    }

    /** documented but not currently implemented at archive.org */
    @SuppressWarnings("unchecked")
    public <T extends Temporal> Details<T> getContributorDetails(String contributor, T startDate, T endDate) throws IOException, InterruptedException {
        return client.get(detailsUrl("contributor", contributor, startDate, endDate), null, Details.class);
    }
}