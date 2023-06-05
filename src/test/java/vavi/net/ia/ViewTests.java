package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ViewTests extends Base {

    private static final String item = "adventuresoftoms00twaiiala";
    private static final String[] items = {item, "texts"};
    private static final String collection = "computer-image-corporation-archive";

    private static void validateSummary(Views.Summary view) {
        assertNotNull(view);
        assertTrue(view.hasData);

        assertNotNull(view.last7Days);
        assertNotNull(view.last30Days);
        assertNotNull(view.allTime);
    }

    @Test
    public void getItemSummary() throws IOException, InterruptedException {
        var view = client.views.getItemSummary(item, false);
        validateSummary(view);
        assertNull(view.detail);

        view = client.views.getItemSummary(item, /*legacy:*/ true);
        validateSummary(view);
        assertNull(view.detail);

        var views = client.views.getItemSummary(items, false);
        assertEquals(2, views.size());
        var summary = views.values().stream().findFirst();
        assertTrue(summary.isPresent());
        validateSummary(summary.get());
    }

    private static <T> void validatePerDay(Views.SummaryPerDay<T> details) {
        assertNotNull(details);
        assertNotNull(details.days);

        var summary = details.ids.values().stream().findFirst();
        assertTrue(summary.isPresent());
        validateSummary(summary.get());
        validateDetail(summary.get().detail);
    }

    static void validateDetail(Views.SummaryDetail detail) {
        assertNotNull(detail);
        assertNotNull(detail.pre2017Total);

        validateStats(detail.robot);
        validateStats(detail.nonRobot);
        validateStats(detail.unrecognized);
        validateStats(detail.pre2017);
    }

    static void validateStats(Views.SummaryDetailStats stats) {
        assertNotNull(stats);
        assertNotNull(stats.perDay);
        assertNotNull(stats.sumPerDay);
        assertNotNull(stats.previousDaysTotal);
    }

    @Test
    public void getItemSummaryPerDay() throws IOException, InterruptedException {
        var perDayDateTime = client.views.<LocalDateTime>getItemSummaryPerDay(item);
        validatePerDay(perDayDateTime);
        assertEquals(1, perDayDateTime.ids.size());

        var perDayString = client.views.<String>getItemSummaryPerDay(item);
        validatePerDay(perDayString);
        assertEquals(1, perDayString.ids.size());

        var perDayLocalDate = client.views.<LocalDate>getItemSummaryPerDay(items);
        validatePerDay(perDayLocalDate);
        assertEquals(2, perDayLocalDate.ids.size());
    }

    private static <T> void validateDetails(Views.Details<T> details) {
        assertNotNull(details);
        assertNotNull(details.days);
        assertNotNull(details.counts);

        var count = details.counts.stream().findFirst().orElseGet(Views.Details.GeoCount::new);
        assertNotNull(count);
        assertNotNull(count.count);
        assertNotNull(count.countKind);
        assertNotNull(count.country);
        assertNotNull(count.geoCountry);
        assertNotNull(count.geoState);
        assertNotNull(count.kind);
        assertNotNull(count.latitude);
        assertNotNull(count.longitude);
        assertNotNull(count.state);

        assertNotNull(details.referers);
        if (details.referers.size() > 0) {
            var referer = details.referers.stream().findFirst().get();
            assertNotNull(referer.kind);
            assertNotNull(referer.referer);
            assertNotNull(referer.score);
        }
    }

    @Test
    public void getItemDetails() throws IOException, InterruptedException {
        var details = client.views.getItemDetails(item, startDateTime, endDateTime);
        validateDetails(details);

        var details2 = client.views.getItemDetails(item, startDate, endDate);
        validateDetails(details2);
    }

    @Test
    public void getCollectionDetails() throws IOException, InterruptedException {
        var details = client.views.getCollectionDetails(collection, startDateTime, endDateTime);
        validateDetails(details);

        var details2 = client.views.getCollectionDetails(collection, startDate, endDate);
        validateDetails(details2);
    }

//#if LATER // documented but not currently implemented at archive.org
//    @Test
//    public void getContributorDetails() {
//        var details = client.views.getContributorDetails(_contributor, startDateTime, endDateTime);
//        validateDetails(details);
//
//        var details2 = client.views.getContributorDetails(_contributor, startDate, endDate);
//        validateDetails(details2);
//    }
//#endif
}