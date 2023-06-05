package vavi.net.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.SerializedName;


public class Reviews {

    private static String url(String identifier) {
        return "https://archive.org/services/reviews.php?identifier=" + identifier;
    }

    private final Client client;

    public Reviews(Client client) {
        this.client = client;
    }

    public static class GetResponse extends ServerResponse {

        public Value_ value;

        public static class Value_ {

            @SerializedName("reviewtitle")
            public String title;

            @SerializedName("reviewbody")
            public String body;

            public String reviewer;

            @SerializedName("reviewer_itemname")
            public String reviewerItemName;

            @SerializedName("createdate")
            public LocalDateTime dateCreated;

            @SerializedName("reviewdate")
            public LocalDateTime dateModified;

            //@JsonAdapter(JsonConverters.NumberAdapter.class)
            public int stars;
        }
    }

    public GetResponse get(String identifier) throws IOException, InterruptedException {
        var response = client.<GetResponse>get(url(identifier), null, GetResponse.class);
        response.ensureSuccess();
        return response;
    }

    public static class AddOrUpdateResponse extends ServerResponse {

        public Value_ value;

        public static class Value_ {

            @SerializedName("task_id")
            public long taskId;
            @SerializedName("review_updated")
            public boolean reviewUpdated;
        }
    }

    public static class AddOrUpdateRequest {

        @JsonIgnore
        public String identifier;

        public String title;
        public String body;
        public int stars;
    }

    public AddOrUpdateResponse addOrUpdate(AddOrUpdateRequest request) throws IOException, InterruptedException {
        if (request.identifier == null) throw new IllegalArgumentException("identifier required");

        var response = client.send("POST", url(request.identifier), request, AddOrUpdateResponse.class);
        response.ensureSuccess();
        return response;
    }

    public static class DeleteResponse extends ServerResponse {

        public Value_ value;

        public static class Value_ {

            @SerializedName("task_id")
            public long taskId;
        }
    }

    public DeleteResponse delete(String identifier) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url(identifier)))
                .DELETE();

        var response = client.send(httpRequest, DeleteResponse.class);
        response.ensureSuccess();
        return response;
    }
}