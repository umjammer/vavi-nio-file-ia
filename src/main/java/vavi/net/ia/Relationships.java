package vavi.net.ia;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import vavi.util.StringUtil;


public class Relationships {

    private static String url(String identifier) {
        return String.format("https://archive.org/metadata/%s/simplelists", identifier);
    }

    private static final String searchUrl = "https://archive.org/advancedsearch.php";

    private final Client client;

    public Relationships(Client client) {
        this.client = client;
    }

    public static class GetParentsResponse implements Closeable {
        public Map<String, SimpleList> lists = new HashMap<>();
        public String error;

        private boolean disposedValue;

        public void close() {
            if (!disposedValue) {
                lists.clear();
                disposedValue = true;
            }
        }
    }

    public static class SimpleList {
        public JsonObject notes;

        @SerializedName("sys_changed_by")
        public LastChangedBy_ lastChangedBy;

        public static class LastChangedBy_ {
            public String source;
            public String username;

            @SerializedName("task_id")
            public long taskId;

            @Override public String toString() {
                return StringUtil.paramString(this);
            }
        }

        @SerializedName("sys_last_changed")
        public LocalDateTime lastChangedDate;

        @Override public String toString() {
            return StringUtil.paramString(this);
        }
    }

    static class SimpleListResponse {
        public Map<String, Map<String, SimpleList>> result;
        public String error;
    }

    public GetParentsResponse getParents(String identifier) throws IOException, InterruptedException {
        SimpleListResponse simpleListResponse = client.get(url(identifier), null, SimpleListResponse.class);
        GetParentsResponse response = new GetParentsResponse() {{ error = simpleListResponse.error; }};
        if (simpleListResponse.result != null) response.lists = simpleListResponse.result.values().stream().findFirst().get();
        return response;
    }

    public static class GetChildrenResponse {
        public Response_ response;

        public static class Response_ {
            public long numFound;
            public long start;
            public List<Doc> docs;

            public static class Doc {
                public String identifier = null;
            }

            @Override public String toString() {
                return StringUtil.paramString(this);
            }
        }

        public List<String> identifiers() {
            return response.docs.stream().map(x -> x.identifier).collect(Collectors.toList());
        }
    }

    public GetChildrenResponse getChildren(String identifier, String listname/* = null*/, Integer rows/* = null*/, Integer page/* = null*/) throws IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();

        query.put("q", "simplelists__" + (listname != null ? "catchall" : identifier));
        query.put("fl", "identifier");
        query.put("output", "json");

        query.put("rows", rows == 0 ? "*" : String.valueOf(rows));
        if (page != null) query.put("page", String.valueOf(page));

        return client.get(searchUrl, query, GetChildrenResponse.class);
    }

    private static class Patch {
        public String op;
        public String parent;
        public String list;
        public String notes;
    }

    public Metadata.WriteResponse add(String identifier, String parentIdentifier, String listName, String notes/*=null*/) throws IOException, InterruptedException {
        Patch patch = new Patch();
        patch.op = "set"; // set is not a standard verb, so we can't use JsonPatchDocument
        patch.parent = parentIdentifier;
        patch.list = listName;
        patch.notes = notes;

        return client.metadata.write(Metadata.url(identifier), "simplelists", Client.gson.toJson(patch));
    }

    public Metadata.WriteResponse remove(String identifier, String parentIdentifier, String listName) throws IOException, InterruptedException {
        Patch patch = new Patch();
        patch.op = "delete"; // delete is not a standard verb either (should be "remove")
        patch.parent = parentIdentifier;
        patch.list = listName;

        return client.metadata.write(Metadata.url(identifier), "simplelists", Client.gson.toJson(patch));
    }
}