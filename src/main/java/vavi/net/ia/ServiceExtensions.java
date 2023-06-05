package vavi.net.ia;

import java.net.CookieHandler;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;


public class ServiceExtensions {

    public static HttpClient addInternetArchiveServices(Duration timeout/*=null*/) {

        Duration Timeout = timeout != null ? Duration.ofMinutes(15) : null;
        HttpClient.Builder handler = HttpClient.newBuilder();
        handler.followRedirects(HttpClient.Redirect.NORMAL);
        handler.cookieHandler(CookieHandler.getDefault());
//        handler.AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate;
        return handler.build();
    }

    public static void addInternetArchiveDefaultRetryPolicies() {
        Logger logger = Logger.getLogger(Client.class.getName());

        Set<Integer> noRetryCodes = new HashSet<>();
        noRetryCodes.add(504); // ServiceUnavailable
        noRetryCodes.add(429); // TooManyRequests
        noRetryCodes.add(401); // Unauthorized
        noRetryCodes.add(400); // BadRequest
        noRetryCodes.add(404); // NotFound


        Consumer<HttpResponse<?>> retryPolicy = r -> {
            var delays = new Duration[] {Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8)};
            int retryAttempt = 0;
            if (r.statusCode() / 200 != 1 && !noRetryCodes.contains(r.statusCode())) {
                logger.fine(String.format("HTTP status %d retry #%d delay %s", r.statusCode(), retryAttempt, delays[retryAttempt]));
            }
        };

        Consumer<HttpResponse<?>> retryPutPolicy = r -> {
            if (r.statusCode() == 404 && (r.request() != null && Objects.equals(r.request().method(), "PUT"))) {
                var delays = new Duration[] {Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(60)};
                int retryAttempt = 0;
                logger.fine(String.format("HTTP PUT status %d retry #%d delay %s", r.statusCode(), retryAttempt, delays[retryAttempt]));
            }
        };

        Consumer<HttpResponse<?>> serviceUnavailablePolicy = r -> {
            if (r.statusCode() == 504) {
                var delays = new Duration[] {Duration.ofSeconds(60), Duration.ofSeconds(120), Duration.ofSeconds(180)};
                int retryAttempt = 0;
                logger.fine(String.format("HTTP error %d retry #%d delay %s", r.statusCode(), retryAttempt, delays[retryAttempt]));
            }
        };

        Consumer<HttpResponse<?>> tooManyRequestsPolicy = r -> {
            if (r.statusCode() == 429) {
                int retryCount = 3;
                Supplier<Duration> x = () -> {
                    var retryAfter = r.headers().firstValue("RetryAfter");
                    if (retryAfter.isEmpty()) return Duration.ofSeconds(60);

                    var duration = Duration.ofSeconds(Long.parseLong(retryAfter.get()));
                    if (duration == null) return Duration.ofSeconds(60);

                    if (duration.compareTo(Duration.ofMinutes(3)) > 0) duration = Duration.ofMinutes(3);
                    return (Duration) duration;
                };
                int retryAttempt = 0;
                logger.fine(String.format("HTTP error %d retry #%d delay %s", (int) r.statusCode(), retryAttempt, x.get()));
            }
        };
    }
}
