package vavi.net.ia;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import vavi.net.ia.dotnet.KeyValuePair;
import vavi.util.Debug;


/**
 * @see "https://archive.org/developers/metadata.html"
 */
public class Metadata {

    static String url(String identifier) {
        return "https://archive.org/metadata/" + identifier;
    }

    private final Client client;

    public Metadata(Client client) {
        this.client = client;
    }

    public static class ReadResponse {

        @SerializedName("created")
        @JsonAdapter(JsonConverters.UnixEpochDateTimeNullableConverter.class)
        public LocalDateTime dateCreated;

        @SerializedName("d1")
        public String dataNodePrimary = null;

        @SerializedName("d2")
        public String dataNodeSecondary;

        @SerializedName("solo")
        public Boolean dataNodeSolo;

        @SerializedName("is_dark")
        public boolean isDark;

        public String dir = null;

        public List<File> files;

        public static class File {

            public String name;
            public String source;
            public String original;

            @SerializedName("mtime")
            @JsonAdapter(JsonConverters.UnixEpochDateTimeNullableConverter.class)
            public LocalDateTime modificationDate;

            //@JsonAdapter(JsonConverters.NumberAdapter.class)
            public Long size;

            public String md5;
            public String crc32;
            public String sha1;

            public String btih;
            public String summation;
            public String format;

            //@JsonAdapter(JsonConverters.NumberAdapter.class)
            public float length;

            //@JsonAdapter(JsonConverters.NumberAdapter.class)
            public int width;

            //@JsonAdapter(JsonConverters.NumberAdapter.class)
            public int height;

            //@JsonAdapter(JsonConverters.NumberAdapter.class)
            public int rotation;

            @SerializedName("viruscheck")
            @JsonAdapter(JsonConverters.UnixEpochDateTimeNullableConverter.class)
            public LocalDateTime virusCheckDate;
        }

        public JsonObject metadata;

        @SerializedName("item_last_updated")
        @JsonAdapter(JsonConverters.UnixEpochDateTimeNullableConverter.class)
        public LocalDateTime dateLastUpdated;

        @SerializedName("item_size")
        public Long size;

        public Long uniq;

        @SerializedName("servers_unavailable")
        public boolean serversUnavailable;

        @SerializedName("workable_servers")
        public List<String> workableServers;
    }

    public ReadResponse read(String identifier) throws IOException, InterruptedException {
        return client.get(url(identifier), null, ReadResponse.class);
    }

    public static class WriteResponse extends ServerResponse {

        @SerializedName("task_id")
        public Long taskId;

        public String log;
        public String error;
    }

    WriteResponse write(String url, String target, String json) throws IOException, InterruptedException {
        List<KeyValuePair<String, String>> formData = new ArrayList<>();
        formData.add(new KeyValuePair<>("-target", target));
        formData.add(new KeyValuePair<>("-patch", json));

        String form = formData.stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
Debug.println(Level.FINER, "post: " + form);
        HttpRequest.BodyPublisher httpContent = HttpRequest.BodyPublishers.ofString(form);
        WriteResponse writeMetadataResponse = client.send("POST", url, "application/x-www-form-urlencoded", httpContent, WriteResponse.class);

        writeMetadataResponse.ensureSuccess();
        return writeMetadataResponse;
    }

    public WriteResponse write(String identifier, String patch) throws IOException, InterruptedException {
Debug.println(Level.FINER, "patch: " + patch);
        return write(url(identifier), "metadata", patch);
    }
}