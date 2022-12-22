package vavi.net.ia;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ViewTests extends Base {

    private static final String _item = "adventuresoftoms00twaiiala";
    private static final String[] _items = {_item, "texts"};
    private static final String _collection = "computer-image-corporation-archive";

    private static void ValidateSummary(Views.Summary view) {
        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.HasData);

        Assertions.assertNotNull(view.Last7Days);
        Assertions.assertNotNull(view.Last30Days);
        Assertions.assertNotNull(view.AllTime);
    }

    @Test
    public void GetItemSummary() throws IOException, InterruptedException {
        var view = _client.Views.GetItemSummaryAsync(_item, false);
        ValidateSummary(view);
        Assertions.assertNull(view.Detail);

        view = _client.Views.GetItemSummaryAsync(_item, /*legacy:*/ true);
        ValidateSummary(view);
        Assertions.assertNull(view.Detail);

        var views = _client.Views.GetItemSummaryAsync(_items, false);
        Assertions.assertEquals(2, views.size());
        ValidateSummary(views.values().stream().findFirst().get());
    }

    private static <T> void ValidatePerDay(Views.SummaryPerDay<T> details) {
        Assertions.assertNotNull(details);
        Assertions.assertNotNull(details.Days);

        var summary = details.Ids.values().stream().findFirst().get();
        ValidateSummary(summary);
        ValidateDetail(summary.Detail);
    }

    static void ValidateDetail(Views.SummaryDetail detail) {
        Assertions.assertNotNull(detail);
        Assertions.assertNotNull(detail.Pre2017Total);

        ValidateStats(detail.Robot);
        ValidateStats(detail.NonRobot);
        ValidateStats(detail.Unrecognized);
        ValidateStats(detail.Pre2017);
    }

    static void ValidateStats(Views.SummaryDetailStats stats) {
        Assertions.assertNotNull(stats);
        Assertions.assertNotNull(stats.PerDay);
        Assertions.assertNotNull(stats.SumPerDay);
        Assertions.assertNotNull(stats.PreviousDaysTotal);
    }

    @Test
    public void GetItemSummaryPerDayAsync() throws IOException, InterruptedException {
        var perDayDateTime = _client.Views.<LocalDateTime>GetItemSummaryPerDayAsync(_item);
        ValidatePerDay(perDayDateTime);
        Assertions.assertEquals(1, perDayDateTime.Ids.size());

        var perDayString = _client.Views.<String>GetItemSummaryPerDayAsync(_item);
        ValidatePerDay(perDayString);
        Assertions.assertEquals(1, perDayString.Ids.size());

        var perDayLocalDate = _client.Views.<LocalDate>GetItemSummaryPerDayAsync(_items);
        ValidatePerDay(perDayLocalDate);
        Assertions.assertEquals(2, perDayLocalDate.Ids.size());
    }

    private static <T> void ValidateDetails(Views.Details<T> details) {
        Assertions.assertNotNull(details);
        Assertions.assertNotNull(details.Days);
        Assertions.assertNotNull(details.Counts);

        var count = details.Counts.stream().findFirst().orElseGet(Views.Details.GeoCount::new);
        Assertions.assertNotNull(count);
        Assertions.assertNotNull(count.Count);
        Assertions.assertNotNull(count.CountKind);
        Assertions.assertNotNull(count.Country);
        Assertions.assertNotNull(count.GeoCountry);
        Assertions.assertNotNull(count.GeoState);
        Assertions.assertNotNull(count.Kind);
        Assertions.assertNotNull(count.Latitude);
        Assertions.assertNotNull(count.Longitude);
        Assertions.assertNotNull(count.State);

        Assertions.assertNotNull(details.Referers);
        if (details.Referers.size() > 0) {
            var referer = details.Referers.stream().findFirst().get();
            Assertions.assertNotNull(referer.Kind);
            Assertions.assertNotNull(referer.Referer);
            Assertions.assertNotNull(referer.Score);
        }
    }

    @Test
    public void GetItemDetailsAsync() throws IOException, InterruptedException {
        var details = _client.Views.GetItemDetailsAsync(_item, _startDateTime, _endDateTime);
        ValidateDetails(details);

        var details2 = _client.Views.GetItemDetailsAsync(_item, _startLocalDate, _endLocalDate);
        ValidateDetails(details2);
    }

    @Test
    public void GetCollectionDetailsAsync() throws IOException, InterruptedException {
        var details = _client.Views.GetCollectionDetailsAsync(_collection, _startDateTime, _endDateTime);
        ValidateDetails(details);

        var details2 = _client.Views.GetCollectionDetailsAsync(_collection, _startLocalDate, _endLocalDate);
        ValidateDetails(details2);
    }

//#if LATER // documented but not currently implemented at archive.org
//    @Test
//    public void GetContributorDetailsAsync() {
//        var details = _client.Views.GetContributorDetailsAsync(_contributor, _startDateTime, _endDateTime);
//        ValidateDetails(details);
//
//        var details2 = _client.Views.GetContributorDetailsAsync(_contributor, _startLocalDate, _endLocalDate);
//        ValidateDetails(details2);
//    }
//#endif
}