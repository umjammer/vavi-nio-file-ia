package vavi.net.ia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


public class Views {

    private static final String Url = "https://be-api.us.archive.org/views/v1";

    private static <T> String DetailsUrl(String type, String id, T startDate, T endDate) {
        return "{Url}/detail/{type}/{UrlEncode(id)}/{startDate:yyyy-MM-dd}/{endDate:yyyy-MM-dd}";
    }

    private final Client _client;

    public Views(Client client) {
        _client = client;
    }

    public static class Summary {
        @JacksonXmlProperty(localName = "have_data")
        public boolean HasData;

        @JacksonXmlProperty(localName = "last_7day")
        public long Last7Days;

        @JacksonXmlProperty(localName = "last_30day")
        public long Last30Days;

        @JacksonXmlProperty(localName = "all_time")
        public long AllTime;

        public SummaryDetail Detail;
    }

    public static class SummaryDetail {
        @JacksonXmlProperty(localName = "pre_20170101_total")
        public long Pre2017Total;

        @JacksonXmlProperty(localName = "non_robot")
        public SummaryDetailStats NonRobot;

        public SummaryDetailStats Robot;
        public SummaryDetailStats Unrecognized;
        public SummaryDetailStats Pre2017;
    }

    public static class SummaryDetailStats {
        @JacksonXmlProperty(localName = "per_day")
        public List<Long> PerDay = new ArrayList<>();

        @JacksonXmlProperty(localName = "previous_days_total")
        public long PreviousDaysTotal;

        @JacksonXmlProperty(localName = "sum_per_day_data")
        public long SumPerDay;
    }

    public Summary GetItemSummaryAsync(String identifier, boolean legacy/* = false*/) throws IOException, InterruptedException {
        Map<String, Views.Summary> summaries = GetItemSummaryAsync(new String[] {identifier}, legacy);
        if (summaries.size() == 0) throw new IllegalStateException("identifier not found");
        return summaries.values().stream().findFirst().get();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Summary> GetItemSummaryAsync(String[] identifiers, boolean legacy/* = false*/) throws IOException, InterruptedException {
        String api = legacy ? "legacy_counts" : "short";
        return _client.GetAsync(String.format("%s/%s/%s", Url, api, String.join(", ", identifiers)), null, Map.class);
    }

    public static class SummaryPerDay<T> {
        public List<T> Days = new ArrayList<>();
        public Map<String, Summary> Ids = new HashMap<>();
    }

    public <T> SummaryPerDay<T> GetItemSummaryPerDayAsync(String identifier) throws IOException, InterruptedException {
        return this.GetItemSummaryPerDayAsync(new String[] {identifier});
    }

    @SuppressWarnings("unchecked")
    public <T> SummaryPerDay<T> GetItemSummaryPerDayAsync(String[] identifiers) throws IOException, InterruptedException {
        return _client.GetAsync(Url + "/long/" + String.join(", ", identifiers), null, SummaryPerDay.class);
    }

    public static class Details<T> {
        @JacksonXmlProperty(localName = "counts_geo")
        public List<GeoCount> Counts = new ArrayList<>();
        public List<T> Days = new ArrayList<>();

        public List<Referer_> Referers = new ArrayList<>();

        public static class GeoCount {
            @JacksonXmlProperty(localName = "count_kind")
            public String CountKind;

            public String Country;

            @JacksonXmlProperty(localName = "geo_country")
            public String GeoCountry;

            @JacksonXmlProperty(localName = "geo_state")
            public String GeoState;

            @JacksonXmlProperty(localName = "lat")
            public float Latitude;

            @JacksonXmlProperty(localName = "lng")
            public float Longitude;

            public String State;

            @JacksonXmlProperty(localName = "sum_count_value")
            public long Count;

            @JacksonXmlProperty(localName = "ua_kind")
            public String Kind;
        }

        public static class Referer_ {
            public String Referer;
            public long Score;

            @JacksonXmlProperty(localName = "ua_kind")
            public String Kind;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Details<T> GetItemDetailsAsync(String identifier, T startDate, T endDate) throws IOException, InterruptedException {
        return _client.GetAsync(DetailsUrl("item", identifier, startDate, endDate), null, Details.class);
    }

    @SuppressWarnings("unchecked")
    public <T> Details<T> GetCollectionDetailsAsync(String collection, T startDate, T endDate) throws IOException, InterruptedException {
        return _client.GetAsync(DetailsUrl("collection", collection, startDate, endDate), null, Details.class);
    }

    /** documented but not currently implemented at archive.org */
    @SuppressWarnings("unchecked")
    <T> Details<T> GetContributorDetailsAsync(String contributor, T startDate, T endDate) throws IOException, InterruptedException {
        return _client.GetAsync(DetailsUrl("contributor", contributor, startDate, endDate), null, Details.class);
    }
}