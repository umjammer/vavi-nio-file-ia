package vavi.net.ia;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;


public class Relationships {
    private static String Url(String identifier) {
        return String.format("https://archive.org/metadata/%s/simplelists", identifier);
    }

    private static final String SearchUrl = "https://archive.org/advancedsearch.php";

    private final Client _client;

    public Relationships(Client client) {
        _client = client;
    }

    public static class GetParentsResponse implements Closeable {
        public Map<String, SimpleList> Lists = new HashMap<>();
        public String Error;

        private boolean disposedValue;

        public void close() {
            if (!disposedValue) {
                Lists.clear();
                disposedValue = true;
            }
        }
    }

    public static class SimpleList implements Closeable {
        public JsonObject Notes;

        @JacksonXmlProperty(localName = "sys_changed_by")
        public LastChangedBy_ LastChangedBy;

        public static class LastChangedBy_ {
            public String Source;
            public String Username;

            @JacksonXmlProperty(localName = "task_id")
            //@JsonNumberHandling(JsonNumberHandling.AllowReadingFromString)
            public long TaskId;
        }

        @JacksonXmlProperty(localName = "sys_last_changed")
        @JsonAdapter(LocalDateTimeNullableDeserializer.class)
        public LocalDateTime LastChangedDate;

        public void close() {
//            Notes.close();
            Notes = null;
        }
    }

    static class SimpleListResponse implements Closeable {
        public Map<String, Map<String, SimpleList>> Result;
        public String Error;

        public void close() {
//            Result.GetEnumerator().Dispose();
            Result = null;
        }
    }

    public GetParentsResponse GetParentsAsync(String identifier) throws IOException, InterruptedException {
        SimpleListResponse simpleListResponse = _client.GetAsync(Url(identifier), null, SimpleListResponse.class);
        GetParentsResponse response = new GetParentsResponse() {{ Error = simpleListResponse.Error; }};
        if (simpleListResponse.Result != null) response.Lists = simpleListResponse.Result.values().stream().findFirst().get();
        simpleListResponse.close();
        return response;
    }

    public static class GetChildrenResponse {
        public Response_ Response;

        public static class Response_ {
            public long NumFound;
            public long Start;
            public List<Doc> Docs;

            public static class Doc {
                public String Identifier = null;
            }
        }

        public List<String> Identifiers() {
            return Response.Docs.stream().map(x -> x.Identifier).collect(Collectors.toList());
        }
    }

    public GetChildrenResponse GetChildrenAsync(String identifier, String listname/* = null*/, Integer rows/* = null*/, Integer page/* = null*/) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();

        query.put("q", "simplelists__" + (listname != null ? "catchall" : identifier));
        query.put("fl", "identifier");
        query.put("output", "json");

        query.put("rows", rows == 0 ? "*" : String.valueOf(rows));
        if (page != null) query.put("page", String.valueOf(page));

        return _client.GetAsync(SearchUrl, query, GetChildrenResponse.class);
    }

    private static class Patch {
        public String Op;
        public String Parent;
        public String List;
        public String Notes;
    }

    public Metadata.WriteResponse AddAsync(String identifier, String parentIdentifier, String listName, String notes/*=null*/) throws IOException, InterruptedException {
        Patch patch = new Patch();
        patch.Op = "set"; // set is not a standard verb, so we can't use JsonPatchDocument
        patch.Parent = parentIdentifier;
        patch.List = listName;
        patch.Notes = notes;

        return _client.Metadata.WriteAsync(Metadata.Url(identifier), "simplelists", Client._json.toJson(patch));
    }

    public Metadata.WriteResponse RemoveAsync(String identifier, String parentIdentifier, String listName) throws IOException, InterruptedException {
        Patch patch = new Patch();
        patch.Op = "delete"; // delete is not a standard verb either (should be "remove")
        patch.Parent = parentIdentifier;
        patch.List = listName;

        return _client.Metadata.WriteAsync(Metadata.Url(identifier), "simplelists", Client._json.toJson(patch));
    }
}