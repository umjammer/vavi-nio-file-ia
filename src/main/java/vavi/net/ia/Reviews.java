package vavi.net.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.annotations.JsonAdapter;


public class Reviews {

    private static String Url(String identifier) {
        return "https://archive.org/services/reviews.phpidentifier={identifier}";
    }

    private final Client _client;

    public Reviews(Client client) {
        _client = client;
    }

    public static class GetResponse extends ServerResponse {

        public Value_ Value;

        public static class Value_ {

            @JacksonXmlProperty(localName = "reviewtitle")
            public String Title;

            @JacksonXmlProperty(localName = "reviewbody")
            public String Body;

            public String Reviewer;

            @JacksonXmlProperty(localName = "reviewer_itemname")
            public String ReviewerItemName;

            @JacksonXmlProperty(localName = "createdate")
            @JsonAdapter(LocalDateTimeNullableDeserializer.class)
            public LocalDateTime DateCreated;

            @JacksonXmlProperty(localName = "reviewdate")
            @JsonAdapter(LocalDateTimeNullableDeserializer.class)
            public LocalDateTime DateModified;

            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public int Stars;
        }
    }

    public GetResponse GetAsync(String identifier) throws IOException, InterruptedException {
        var response = _client.<GetResponse>GetAsync(Url(identifier), null, GetResponse.class);
        response.EnsureSuccess();
        return response;
    }

    public static class AddOrUpdateResponse extends ServerResponse {

        public Value_ Value;

        public static class Value_ {

            @JacksonXmlProperty(localName = "task_id")
            public long TaskId;
            @JacksonXmlProperty(localName = "review_updated")
            public boolean ReviewUpdated;
        }
    }

    public static class AddOrUpdateRequest {

        @JsonIgnore
        public String Identifier;

        public String Title;
        public String Body;
        public int Stars;
    }

    public AddOrUpdateResponse AddOrUpdateAsync(AddOrUpdateRequest request) throws IOException, InterruptedException {
        if (request.Identifier == null) throw new IllegalArgumentException("identifier required");

        var response = _client.SendAsync("POST", Url(request.Identifier), request, AddOrUpdateResponse.class);
        if (!response.Success) {
            throw new IOException("AddOrUpdateAsync");
        }
        return response;
    }

    public static class DeleteResponse extends ServerResponse {

        public Value_ Value;

        public static class Value_ {

            @JacksonXmlProperty(localName = "task_id")
            public long TaskId;
        }
    }

    public DeleteResponse DeleteAsync(String identifier) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(identifier))
                .DELETE();

        var response = _client.SendAsync(httpRequest, DeleteResponse.class);
        response.EnsureSuccess();
        return response;
    }
}